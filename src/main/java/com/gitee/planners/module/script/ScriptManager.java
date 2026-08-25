package com.gitee.planners.module.script;

import com.gitee.scriptengine.api.ScriptFunction;
import com.gitee.scriptengine.api.CompiledScript;
import com.gitee.scriptengine.api.ContextPreset;
import com.gitee.scriptengine.api.HostAccessMode;
import com.gitee.scriptengine.api.ScriptResult;
import com.gitee.scriptengine.api.ScriptSession;
import com.gitee.scriptengine.api.ScriptSource;
import com.gitee.scriptengine.api.ScriptValue;
import com.gitee.scriptengine.api.ScriptWorkspace;
import com.gitee.scriptengine.api.WorkspaceConfig;
import com.gitee.scriptengine.loader.WorkspaceConfigLoader;
import com.gitee.scriptengine.runtime.ScriptWorkspaces;
import com.gitee.planners.module.script.api.StateAPI;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import taboolib.platform.BukkitPlugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
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
        workspace = ScriptWorkspaces.create(scriptDir, new WorkspaceConfig(
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
     * 调用已装载脚本会话中的函数。
     *
     * @param session 已打开的脚本会话。
     * @param functionName 已定义的函数名。
     * @param args 函数参数。
     * @return 函数返回值。
     */
    public static Object invoke(ScriptSession session, String functionName, Object... args) {
        ScriptResult result = session.invoke(functionName, adaptArguments(args));
        checkResult("执行脚本函数失败: " + functionName, result);
        return result.getValue();
    }

    /**
     * 将配置表达式编译成零参数 JavaScript 函数。
     *
     * <p>表达式文本只在首次装入会话时通过 {@code eval} 声明函数；后续执行全部经由
     * {@link #invokeCompiled(ScriptSession, CompiledScript, Object...)} 调用，禁止把表达式文本直接交给运行时执行。</p>
     *
     * @param id 业务侧稳定标识。
     * @param expression JavaScript 表达式。
     * @return 可跨会话复用的编译函数描述。
     */
    public static CompiledScript compileExpression(String id, String expression) {
        return CompiledScript.Companion.expression(id, expression);
    }

    /**
     * 将 JavaScript 语句块编译成零参数函数。
     *
     * @param id 业务侧稳定标识。
     * @param action JavaScript 语句块。
     * @return 可跨会话复用的编译函数描述。
     */
    public static CompiledScript compileAction(String id, String action) {
        return CompiledScript.Companion.action(id, action);
    }

    /**
     * 在独立会话中调用已编译函数。
     *
     * @param function 已编译函数。
     * @param options 本次执行上下文。
     * @param args 调用参数。
     * @return 函数返回值。
     */
    public static Object invokeCompiled(CompiledScript function, ScriptOptions options, Object... args) {
        ensureInit();
        Map<String, Object> previous = ScriptContext.getCurrent();
        Map<String, Object> variables = createScriptVariables(options.getVariables());
        ScriptContext.setCurrent(variables);
        ManagedSession session = createSession(variables, options.getPreludeScripts());
        try {
            return invokeCompiled(session, function, args);
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
     * 在已打开会话中调用已编译函数。
     *
     * <p>每个函数在同一会话内仅安装一次，之后只通过函数调用执行。</p>
     *
     * @param session 当前 ScriptManager 会话。
     * @param function 已编译函数。
     * @param args 调用参数。
     * @return 函数返回值。
     */
    public static Object invokeCompiled(ScriptSession session, CompiledScript function, Object... args) {
        ScriptResult result = session.invoke(function, adaptArguments(args));
        checkResult("执行预编译脚本函数失败: " + function.getFunctionName(), result);
        return result.getValue();
    }

    /**
     * 将已编译函数安装到会话。
     *
     * @param session 当前 ScriptManager 会话。
     * @param function 已编译函数。
     */
    public static void installCompiledFunction(ScriptSession session, CompiledScript function) {
        ScriptResult result = session.install(function);
        checkResult("安装预编译脚本函数失败: " + function.getFunctionName(), result);
    }

    /**
     * 打开会话（状态回调等跨调用场景）。
     */
    public static ScriptSession openSession(ScriptOptions options) {
        ensureInit();
        return createSession(createSessionVariables(options.getVariables()), options.getPreludeScripts());
    }

    /**
     * 打开一个可在当前请求内反复重绑定的脚本会话。
     *
     * <p>会话不得跨玩家、跨请求或跨线程保存。每次重绑定都会清理上一次声明的临时变量，
     * 调用方仍必须调用 {@link ScriptSession#close()}。</p>
     *
     * @param options 首次执行使用的选项。
     * @param transientBindings 会话执行期间可能写入全局作用域的临时变量名。
     * @return 可重绑定的脚本会话。
     */
    public static ScriptSession openReusableSession(ScriptOptions options, Set<String> transientBindings) {
        ensureInit();
        return createSession(createSessionVariables(options.getVariables()), options.getPreludeScripts(), transientBindings);
    }

    /**
     * 将可重绑定会话切换到新的脚本执行选项。
     *
     * @param session 由 {@link #openReusableSession(ScriptOptions, Set)} 创建的会话。
     * @param options 本次执行使用的选项。
     * @param transientBindings 本次执行可能写入全局作用域的临时变量名。
     */
    public static void rebindReusableSession(ScriptSession session, ScriptOptions options, Set<String> transientBindings) {
        if (!(session instanceof ManagedSession)) {
            throw new IllegalArgumentException("Session is not managed by ScriptManager");
        }
        Map<String, Object> variables = createSessionVariables(options.getVariables());
        ManagedSession managedSession = (ManagedSession) session;
        managedSession.rebind(variables, transientBindings);
    }

    /**
     * 在可重绑定会话中替换一个临时全局变量。
     *
     * <p>调用方必须已通过 {@link #rebindReusableSession(ScriptSession, ScriptOptions, Set)}
     * 绑定当前执行上下文。该变量会在下一次重绑定时清理。</p>
     *
     * @param session 由 {@link #openReusableSession(ScriptOptions, Set)} 创建的会话。
     * @param key 变量名。
     * @param value 变量值。
     */
    public static void setReusableSessionBinding(ScriptSession session, String key, Object value) {
        if (!(session instanceof ManagedSession)) {
            throw new IllegalArgumentException("Session is not managed by ScriptManager");
        }
        ManagedSession managedSession = (ManagedSession) session;
        managedSession.bind(key, value);
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
        return createSession(variables, preludeScripts, java.util.Collections.emptySet());
    }

    private static ManagedSession createSession(Map<String, Object> variables, java.util.List<String> preludeScripts, Set<String> transientBindings) {
        com.gitee.scriptengine.api.ScriptContext context = workspace.createContext(variables, preludeScripts);
        ManagedSession session = new ManagedSession(context, variables, transientBindings);
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
        private final com.gitee.scriptengine.api.ScriptContext context;
        private final Map<String, Object> variables;
        private final Set<String> bindingKeys = new java.util.LinkedHashSet<>();
        private final Map<Integer, ScheduledTask> tasks = new ConcurrentHashMap<>();
        private final AtomicInteger nextTaskId = new AtomicInteger(1);
        private boolean closeRequested;
        private boolean closed;

        private ManagedSession(com.gitee.scriptengine.api.ScriptContext context, Map<String, Object> variables, Set<String> transientBindings) {
            this.delegate = new ContextSession(context);
            this.context = context;
            this.variables = variables;
            bindingKeys.addAll(variables.keySet());
            bindingKeys.addAll(transientBindings);
        }

        @Override
        public ScriptResult eval(String source) {
            return delegate.eval(source);
        }

        @Override
        public ScriptResult eval(ScriptSource source) {
            return delegate.eval(source);
        }

        @Override
        public ScriptResult install(CompiledScript script) {
            return delegate.install(script);
        }

        @Override
        public ScriptResult invoke(String name, Object... args) {
            return delegate.invoke(name, args);
        }

        @Override
        public ScriptResult invoke(CompiledScript script, Object... args) {
            return delegate.invoke(script, args);
        }

        @Override
        public boolean hasFunction(String name) {
            return delegate.hasFunction(name);
        }

        private void rebind(Map<String, Object> nextVariables, Set<String> transientBindings) {
            synchronized (this) {
                if (closed || closeRequested) {
                    throw new IllegalStateException("Session 已关闭或正在关闭，不能重新绑定");
                }
                for (String key : bindingKeys) {
                    context.getBindings().removeMember(key);
                }
                variables.clear();
                variables.putAll(nextVariables);
                bindingKeys.clear();
                bindingKeys.addAll(nextVariables.keySet());
                bindingKeys.addAll(transientBindings);
                for (Map.Entry<String, Object> entry : nextVariables.entrySet()) {
                    context.getBindings().putMember(entry.getKey(), entry.getValue());
                }
            }
        }

        private void bind(String key, Object value) {
            synchronized (this) {
                if (closed || closeRequested) {
                    throw new IllegalStateException("Session 已关闭或正在关闭，不能绑定变量");
                }
                variables.put(key, value);
                bindingKeys.add(key);
                context.getBindings().putMember(key, value);
            }
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

        private static final class ContextSession implements ScriptSession {

            private final com.gitee.scriptengine.api.ScriptContext context;
            private final Set<String> installedScripts = new java.util.LinkedHashSet<>();
            private boolean closed;

            private ContextSession(com.gitee.scriptengine.api.ScriptContext context) {
                this.context = context;
            }

            @Override
            public ScriptResult eval(String source) {
                checkClosed();
                return context.eval(source);
            }

            @Override
            public ScriptResult eval(ScriptSource source) {
                checkClosed();
                return context.eval(source);
            }

            @Override
            public ScriptResult install(CompiledScript script) {
                checkClosed();
                if (installedScripts.contains(script.getFunctionName())) {
                    return new ScriptResult(null, true, null);
                }
                ScriptResult result = eval(script.getSource());
                if (result.getSuccess()) {
                    installedScripts.add(script.getFunctionName());
                }
                return result;
            }

            @Override
            public ScriptResult invoke(String name, Object... arguments) {
                checkClosed();
                ScriptValue function = context.getBindings().getMember(name);
                if (function == null) {
                    return new ScriptResult(null, false, new NoSuchElementException("function '" + name + "' not found"));
                }
                if (!function.canExecute()) {
                    return new ScriptResult(null, false, new IllegalStateException("'" + name + "' is not callable"));
                }
                try {
                    return new ScriptResult(unwrapValue(function.execute(arguments)), true, null);
                } catch (Exception exception) {
                    return new ScriptResult(null, false, exception);
                }
            }

            @Override
            public ScriptResult invoke(CompiledScript script, Object... arguments) {
                ScriptResult installResult = install(script);
                if (!installResult.getSuccess()) {
                    return installResult;
                }
                return invoke(script.getFunctionName(), arguments);
            }

            @Override
            public boolean hasFunction(String name) {
                checkClosed();
                ScriptValue function = context.getBindings().getMember(name);
                if (function == null) {
                    return false;
                }
                return function.canExecute();
            }

            @Override
            public void close() {
                if (closed) {
                    return;
                }
                closed = true;
                installedScripts.clear();
                context.close();
            }

            private void checkClosed() {
                if (closed) {
                    throw new IllegalStateException("Session 已关闭");
                }
            }
        }
    }
}
