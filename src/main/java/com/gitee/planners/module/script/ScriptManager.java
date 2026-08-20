package com.gitee.planners.module.script;

import com.gitee.scriptengine.api.ScriptFunction;
import com.gitee.scriptengine.api.ContextPreset;
import com.gitee.scriptengine.api.HostAccessMode;
import com.gitee.scriptengine.api.ScriptResult;
import com.gitee.scriptengine.api.ScriptSession;
import com.gitee.scriptengine.api.ScriptValue;
import com.gitee.scriptengine.api.ScriptWorkspace;
import com.gitee.scriptengine.api.WorkspaceConfig;
import com.gitee.scriptengine.core.ScriptSessionImpl;
import com.gitee.scriptengine.core.ScriptWorkspaceImpl;
import com.gitee.scriptengine.loader.PreludeInjector;
import com.gitee.scriptengine.loader.WorkspaceConfigLoader;
import com.gitee.planners.module.script.api.StateAPI;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import taboolib.platform.BukkitPlugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * 脚本管理器（静态门面）。
 *
 * 内部通过 ScriptEngine 的 GraalJS 实现执行 JavaScript。
 */
public final class ScriptManager {

    private static final Logger LOGGER = Logger.getLogger("Script");
    private static final Set<ManagedSession> ACTIVE_SESSIONS = ConcurrentHashMap.newKeySet();
    private static ScriptWorkspace workspace;

    private ScriptManager() {}

    /**
     * 初始化引擎（插件启动时调用）。
     */
    public static void init() {
        if (workspace != null) {
            return;
        }
        // 注册所有全局函数到本地注册表
        ScriptFunctionRegistry.registerAll();

        File scriptDir = new File("plugins/Planners/scripts");
        workspace = new ScriptWorkspaceImpl(scriptDir, new WorkspaceConfig(
            ContextPreset.DEFAULT,
            HostAccessMode.ALL,
            name -> true,
            false,
            WorkspaceConfigLoader.INSTANCE.loadPreludeScripts(scriptDir),
            java.util.Collections.emptyList(),
            java.util.Collections.emptyMap(),
            ScriptManager.class.getClassLoader(),
            false,
            java.util.Collections.emptyMap(),
            java.util.Collections.emptyList()
        ));
        LOGGER.info("[Script] 引擎初始化完成: ScriptEngine");
    }

    /**
     * 执行脚本。
     *
     * 保存并恢复调用方的 ScriptContext，避免嵌套调用清掉外层上下文。
     */
    public static Object eval(String source, ScriptOptions options) {
        ensureInit();
        Map<String, Object> previous = ScriptContext.getCurrent();
        Map<String, Object> variables = createScriptVariables(options.getVariables());
        ScriptContext.setCurrent(variables);
        ManagedSession session = createSession(variables, options.getPreludeScripts());
        try {
            ScriptResult result = session.eval(source);
            checkResult("执行脚本失败", result);
            return result.getValue();
        } finally {
            session.close();
            if (previous != null) {
                ScriptContext.setCurrent(previous);
            } else {
                ScriptContext.clear();
            }
        }
    }

    /**
     * 执行脚本（无上下文变量）。
     */
    public static Object eval(String source) {
        return eval(source, new ScriptOptions());
    }

    /**
     * 打开会话（状态回调等跨调用场景）。
     */
    public static ScriptSession openSession(ScriptOptions options) {
        ensureInit();
        return createSession(createSessionVariables(options.getVariables()), options.getPreludeScripts());
    }

    /**
     * 执行技能 action 中定义的回调函数。
     *
     * <p>该方法统一处理脚本上下文、会话生命周期、action 载入和函数查找。</p>
     *
     * @param source action 脚本源码。
     * @param functionName 回调函数名。
     * @param options 脚本执行选项。
     * @param args 回调函数参数。
     * @return 函数存在并完成调用时返回 true，函数不存在时返回 false。
     */
    public static boolean invokeActionFunction(String source, String functionName, ScriptOptions options, Object... args) {
        ensureInit();
        Map<String, Object> previous = ScriptContext.getCurrent();
        Map<String, Object> variables = createScriptVariables(options.getVariables());
        ScriptContext.setCurrent(variables);
        ManagedSession session = null;
        try {
            session = createSession(createSessionVariables(variables), options.getPreludeScripts());
            ScriptResult evalResult = session.eval(source);
            checkResult("载入 action 脚本失败", evalResult);
            if (!session.hasFunction(functionName)) {
                return false;
            }
            ScriptResult invokeResult = session.invoke(functionName, adaptArguments(args));
            checkResult("执行 action 函数失败: " + functionName, invokeResult);
            return true;
        } finally {
            if (session != null) {
                session.close();
            }
            if (previous != null) {
                ScriptContext.setCurrent(previous);
            } else {
                ScriptContext.clear();
            }
        }
    }

    /**
     * 获取当前引擎。
     */
    public static ScriptWorkspace getWorkspace() {
        ensureInit();
        return workspace;
    }

    /**
     * 关闭引擎（插件卸载时调用）。
     */
    public static void shutdown() {
        for (ManagedSession session : ACTIVE_SESSIONS) {
            session.forceClose();
        }
        if (workspace != null) {
            workspace.close();
            workspace = null;
        }
    }

    private static void ensureInit() {
        if (workspace == null) {
            init();
        }
    }

    private static Map<String, Object> createScriptVariables(Map<String, Object> variables) {
        Map<String, Object> scriptVariables = new LinkedHashMap<>();
        scriptVariables.put("stateAPI", StateAPI.INSTANCE);
        scriptVariables.putAll(variables);
        return scriptVariables;
    }

    private static Map<String, Object> createSessionVariables(Map<String, Object> variables) {
        Map<String, Object> sessionVariables = createScriptVariables(variables);
        return sessionVariables;
    }

    private static ManagedSession createSession(Map<String, Object> variables, java.util.List<String> preludeScripts) {
        com.gitee.scriptengine.api.ScriptContext context = workspace.createContext(variables);
        PreludeInjector.INSTANCE.inject(context, workspace.getFolder(), preludeScripts);
        ManagedSession session = new ManagedSession(context, variables);
        ACTIVE_SESSIONS.add(session);
        installGlobalFunctions(context);
        installTimerFunctions(context, session);
        return session;
    }

    private static void installGlobalFunctions(com.gitee.scriptengine.api.ScriptContext context) {
        for (Map.Entry<String, java.util.function.Function<Object[], Object>> entry : GlobalFunctions.getAll().entrySet()) {
            java.util.function.Function<Object[], Object> function = entry.getValue();
            context.getBindings().putMember(entry.getKey(), (ScriptFunction) values -> {
                Object[] arguments = unwrapArguments(values);
                return function.apply(arguments);
            });
        }
    }

    private static void installTimerFunctions(com.gitee.scriptengine.api.ScriptContext context, ManagedSession session) {
        context.getBindings().putMember("setTimeout", (ScriptFunction) values -> session.schedule(values, false));
        context.getBindings().putMember("setInterval", (ScriptFunction) values -> session.schedule(values, true));
        context.getBindings().putMember("clearTimer", (ScriptFunction) values -> session.cancel(values));
    }

    private static void checkResult(String message, ScriptResult result) {
        if (result.getSuccess()) {
            return;
        }
        Throwable error = result.getError();
        if (error != null) {
            throw new RuntimeException(message + ": " + error.getMessage(), error);
        }
        throw new RuntimeException(message);
    }

    private static Object[] adaptArguments(Object[] args) {
        Object[] adapted = new Object[args.length];
        for (int index = 0; index < args.length; index++) {
            adapted[index] = adaptArgument(args[index]);
        }
        return adapted;
    }

    private static Object adaptArgument(Object arg) {
        return arg;
    }

    private static Object[] unwrapArguments(ScriptValue[] values) {
        Object[] arguments = new Object[values.length];
        for (int index = 0; index < values.length; index++) {
            arguments[index] = unwrapValue(values[index]);
        }
        return arguments;
    }

    private static Object unwrapValue(ScriptValue value) {
        if (value.isNull()) {
            return null;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isString()) {
            return value.asString();
        }
        if (value.isNumber()) {
            if (value.fitsInInt()) {
                return value.asInt();
            }
            if (value.fitsInLong()) {
                return value.asLong();
            }
            return value.asDouble();
        }
        if (value.isHostObject()) {
            return value.asHostObject();
        }
        if (value.hasArrayElements()) {
            int size = Math.toIntExact(value.getArraySize());
            Object[] array = new Object[size];
            for (int index = 0; index < size; index++) {
                array[index] = unwrapValue(value.getArrayElement(index));
            }
            return array;
        }
        if (value.hasMembers()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (String key : value.getMemberKeys()) {
                ScriptValue member = value.getMember(key);
                if (member != null) {
                    map.put(key, unwrapValue(member));
                }
            }
            return map;
        }
        return value;
    }

    private static final class ManagedSession implements ScriptSession {

        private final ScriptSession delegate;
        private final Map<String, Object> variables;
        private final Map<Integer, ScheduledTask> tasks = new ConcurrentHashMap<>();
        private final AtomicInteger nextTaskId = new AtomicInteger(1);
        private boolean closeRequested;
        private boolean closed;

        private ManagedSession(com.gitee.scriptengine.api.ScriptContext context, Map<String, Object> variables) {
            this.delegate = new ScriptSessionImpl(context);
            this.variables = variables;
        }

        @Override
        public ScriptResult eval(String source) {
            return delegate.eval(source);
        }

        @Override
        public ScriptResult invoke(String name, Object... args) {
            return delegate.invoke(name, args);
        }

        @Override
        public boolean hasFunction(String name) {
            return delegate.hasFunction(name);
        }

        @Override
        public void close() {
            synchronized (this) {
                if (closed) {
                    return;
                }
                closeRequested = true;
                if (!tasks.isEmpty()) {
                    return;
                }
                closeNow();
            }
        }

        private int schedule(ScriptValue[] values, boolean repeating) {
            if (values.length < 2) {
                throw new IllegalArgumentException("setTimeout requires callback and delay");
            }
            ScriptValue callback = values[0];
            ScriptValue delayValue = values[1];
            if (!callback.canExecute()) {
                throw new IllegalArgumentException("timer callback must be callable");
            }
            long delay = readNumber(delayValue, "delay");
            if (delay < 0) {
                delay = 0;
            }
            long period = readNumber(delayValue, "period");
            if (repeating && period <= 0) {
                period = 1;
            }
            int taskId = nextTaskId.getAndIncrement();
            ScheduledTask scheduledTask = new ScheduledTask();
            tasks.put(taskId, scheduledTask);
            Runnable action = () -> runTask(taskId, callback, repeating);
            try {
                BukkitTask task;
                if (repeating) {
                    task = Bukkit.getScheduler().runTaskTimer(BukkitPlugin.getInstance(), action, delay, period);
                } else {
                    task = Bukkit.getScheduler().runTaskLater(BukkitPlugin.getInstance(), action, delay);
                }
                scheduledTask.task = task;
            } catch (RuntimeException throwable) {
                tasks.remove(taskId);
                closeIfIdle();
                throw throwable;
            } catch (Error error) {
                tasks.remove(taskId);
                closeIfIdle();
                throw error;
            }
            return taskId;
        }

        private boolean cancel(ScriptValue[] values) {
            if (values.length == 0) {
                return false;
            }
            int taskId = values[0].asInt();
            ScheduledTask task = tasks.remove(taskId);
            if (task == null) {
                return false;
            }
            task.cancel();
            closeIfIdle();
            return true;
        }

        private void runTask(int taskId, ScriptValue callback, boolean repeating) {
            synchronized (this) {
                if (closed) {
                    return;
                }
            }
            Map<String, Object> previous = ScriptContext.getCurrent();
            ScriptContext.setCurrent(variables);
            try {
                callback.executeVoid();
            } catch (Throwable throwable) {
                LOGGER.warning("[Script] 定时脚本执行失败: " + throwable.getMessage());
            } finally {
                if (!repeating) {
                    tasks.remove(taskId);
                }
                if (previous != null) {
                    ScriptContext.setCurrent(previous);
                } else {
                    ScriptContext.clear();
                }
                closeIfIdle();
            }
        }

        private void closeIfIdle() {
            synchronized (this) {
                if (closeRequested && tasks.isEmpty() && !closed) {
                    closeNow();
                }
            }
        }

        private void forceClose() {
            synchronized (this) {
                if (closed) {
                    return;
                }
                for (ScheduledTask task : tasks.values()) {
                    task.cancel();
                }
                tasks.clear();
                closeNow();
            }
        }

        private void closeNow() {
            if (closed) {
                return;
            }
            closed = true;
            ACTIVE_SESSIONS.remove(this);
            delegate.close();
        }

        private static long readNumber(ScriptValue value, String key) {
            if (value == null) {
                return 0L;
            }
            if (value.isNumber()) {
                return value.asLong();
            }
            if (!value.hasMember(key)) {
                return 0L;
            }
            ScriptValue member = value.getMember(key);
            if (member == null || member.isNull()) {
                return 0L;
            }
            return member.asLong();
        }

        private static final class ScheduledTask {

            private volatile BukkitTask task;

            private void cancel() {
                BukkitTask currentTask = task;
                if (currentTask != null) {
                    currentTask.cancel();
                }
            }
        }
    }
}
