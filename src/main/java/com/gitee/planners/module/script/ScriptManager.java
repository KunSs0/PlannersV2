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
import com.gitee.scriptengine.api.ReusableScriptSession;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * 脚本管理器（静态门面）。
 *
 * 内部通过 ScriptEngine 的 GraalJS 实现执行 JavaScript。
 *
 * <p>长期会话与可重绑定会话均基于 ScriptEngine 公开的 {@link ScriptSession} API 实现：
 * 全局函数和上下文变量在会话创建时注入；运行期变更通过会话内的载体数组 +
 * {@code eval} 写入 JS 全局作用域；带参预编译函数在编译期生成从载体解构参数的语句。</p>
 */
public final class ScriptManager {

    private static final Logger LOGGER = Logger.getLogger("Script");
    private static final Set<ManagedSession> ACTIVE_SESSIONS = ConcurrentHashMap.newKeySet();
    private static ScriptWorkspace workspace;

    /** 会话内全局注入的参数/绑定载体数组长度上限。 */
    private static final int CARRIER_SIZE = 64;

    /** 长期会话中用于函数参数传递的载体全局名。 */
    private static final String ARGS_GLOBAL = "__plannersArgs";

    /** 普通会话中用于运行期全局绑定写入的载体全局名。 */
    private static final String CARRIER_GLOBAL = "__plannersCarrier";

    private static volatile ScriptSession persistentSession;
    private static volatile Object[] persistentArguments;

    /** 定时器 ID 分配器（全工作区唯一）。 */
    private static final AtomicInteger TIMER_ID = new AtomicInteger(1);

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
            ContextPreset.SHARED_ENGINE,
            HostAccessMode.ALL,
            name -> true,
            true,
            WorkspaceConfigLoader.INSTANCE.loadPreludeScripts(scriptDir),
            java.util.Collections.emptyList(),
            java.util.Collections.emptyMap(),
            ScriptManager.class.getClassLoader(),
            false,
            java.util.Collections.emptyMap(),
            java.util.Collections.emptyList()
        ));
        warmPersistentSession();
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
     * 将表达式编译为仅依赖函数参数的长期工作区函数。
     *
     * <p>当前 ScriptEngine 公开 API 只提供零参数预编译函数；本方法在函数体前生成
     * 从载体数组解构参数的语句，调用前由 {@link #invokePersistent(CompiledScript, ScriptOptions, Object...)}
     * 把实参写入载体，保证调用参数不会写入 JavaScript 全局作用域。</p>
     */
    public static CompiledScript compileExpression(String id, java.util.List<String> parameters, String expression) {
        String body = destructureParameters(parameters) + "return (" + expression + ");";
        return CompiledScript.Companion.action(id, body);
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

    /** 将语句块编译为仅依赖函数参数的长期工作区函数。语义同 {@link #compileExpression(String, java.util.List, String)}。 */
    public static CompiledScript compileAction(String id, java.util.List<String> parameters, String action) {
        return CompiledScript.Companion.action(id, destructureParameters(parameters) + action);
    }

    /** 生成从载体数组解构命名参数的语句序列。 */
    private static String destructureParameters(java.util.List<String> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "";
        }
        if (parameters.size() > CARRIER_SIZE) {
            throw new IllegalArgumentException("脚本函数参数数量超出限制: " + parameters.size());
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < parameters.size(); index++) {
            builder.append("var ").append(parameters.get(index)).append(" = ")
                .append(ARGS_GLOBAL).append('[').append(index).append("];\n");
        }
        return builder.toString();
    }

    /**
     * 在工作区唯一的长期 Context 内执行参数化预编译函数。
     *
     * 调用参数不会写入 JavaScript 全局作用域；仅当前函数调用栈可见。
     */
    public static Object invokePersistent(CompiledScript function, ScriptOptions options, Object... args) {
        ensureInit();
        ScriptSession session = getPersistentSession();
        Map<String, Object> variables = createScriptVariables(options.getVariables());
        Object[] adapted = adaptArguments(args);
        synchronized (session) {
            Map<String, Object> previous = ScriptContext.getCurrent();
            ScriptContext.setCurrent(variables);
            try {
                System.arraycopy(adapted, 0, persistentArguments, 0, adapted.length);
                ScriptResult result = session.invoke(function);
                checkResult("执行长期预编译脚本函数失败: " + function.getFunctionName(), result);
                return result.getValue();
            } finally {
                if (previous != null) {
                    ScriptContext.setCurrent(previous);
                } else {
                    ScriptContext.clear();
                }
            }
        }
    }

    /**
     * 在长期工作区 Context 内执行参数化预编译函数，并返回分段计时结果。
     *
     * <p>该入口仅用于业务侧显式性能采样。普通调用继续使用
     * {@link #invokePersistent(CompiledScript, ScriptOptions, Object...)}，避免创建统计对象。</p>
     *
     * <p>当前 ScriptEngine 公开 API 不再提供运行时内部分段计时，
     * 仅宿主侧阶段与整体执行耗时可测，其余分段记为 0。</p>
     *
     * @param function 已预编译的函数。
     * @param options 当前脚本选项。
     * @param args 函数参数。
     * @return 包含返回值和各执行阶段纳秒耗时的调用结果。
     */
    public static PersistentInvocation invokePersistentProfiled(CompiledScript function, ScriptOptions options, Object... args) {
        ensureInit();
        ScriptSession session = getPersistentSession();
        long variablesStart = System.nanoTime();
        Map<String, Object> variables = createScriptVariables(options.getVariables());
        long variablesCopyNanos = System.nanoTime() - variablesStart;
        long contextStart = System.nanoTime();
        Map<String, Object> previous = ScriptContext.getCurrent();
        ScriptContext.setCurrent(variables);
        long contextSetNanos = System.nanoTime() - contextStart;
        long argumentsStart = System.nanoTime();
        Object[] adapted = adaptArguments(args);
        long argumentAdaptNanos = System.nanoTime() - argumentsStart;
        long functionExecuteNanos;
        long contextRestoreNanos;
        synchronized (session) {
            try {
                long executeStart = System.nanoTime();
                System.arraycopy(adapted, 0, persistentArguments, 0, adapted.length);
                ScriptResult result = session.invoke(function);
                functionExecuteNanos = System.nanoTime() - executeStart;
                checkResult("执行长期预编译脚本函数失败: " + function.getFunctionName(), result);
                PersistentInvocation invocation = new PersistentInvocation(
                    result.getValue(),
                    variablesCopyNanos,
                    contextSetNanos,
                    argumentAdaptNanos,
                    0L,
                    0L,
                    0L,
                    0L,
                    functionExecuteNanos,
                    0L
                );
                return invocation;
            } finally {
                long restoreStart = System.nanoTime();
                if (previous != null) {
                    ScriptContext.setCurrent(previous);
                } else {
                    ScriptContext.clear();
                }
                contextRestoreNanos = System.nanoTime() - restoreStart;
            }
        }
    }

    /**
     * 长期脚本调用的宿主侧和运行时侧分段计时。
     *
     * 所有耗时均为纳秒，调用方可按一次完整请求汇总后再转换为微秒输出。
     */
    public static final class PersistentInvocation {

        private final Object value;
        private final long variablesCopyNanos;
        private final long contextSetNanos;
        private final long argumentAdaptNanos;
        private final long contextRestoreNanos;
        private final long lockWaitNanos;
        private final long installNanos;
        private final long functionLookupNanos;
        private final long functionExecuteNanos;
        private final long resultUnwrapNanos;

        private PersistentInvocation(
            Object value,
            long variablesCopyNanos,
            long contextSetNanos,
            long argumentAdaptNanos,
            long contextRestoreNanos,
            long lockWaitNanos,
            long installNanos,
            long functionLookupNanos,
            long functionExecuteNanos,
            long resultUnwrapNanos
        ) {
            this.value = value;
            this.variablesCopyNanos = variablesCopyNanos;
            this.contextSetNanos = contextSetNanos;
            this.argumentAdaptNanos = argumentAdaptNanos;
            this.contextRestoreNanos = contextRestoreNanos;
            this.lockWaitNanos = lockWaitNanos;
            this.installNanos = installNanos;
            this.functionLookupNanos = functionLookupNanos;
            this.functionExecuteNanos = functionExecuteNanos;
            this.resultUnwrapNanos = resultUnwrapNanos;
        }

        public Object getValue() {
            return value;
        }

        public long getVariablesCopyNanos() {
            return variablesCopyNanos;
        }

        public long getContextSetNanos() {
            return contextSetNanos;
        }

        public long getContextRestoreNanos() {
            return contextRestoreNanos;
        }

        public long getArgumentAdaptNanos() {
            return argumentAdaptNanos;
        }

        public long getLockWaitNanos() {
            return lockWaitNanos;
        }

        public long getInstallNanos() {
            return installNanos;
        }

        public long getFunctionLookupNanos() {
            return functionLookupNanos;
        }

        public long getFunctionExecuteNanos() {
            return functionExecuteNanos;
        }

        public long getResultUnwrapNanos() {
            return resultUnwrapNanos;
        }
    }

    /**
     * 将无状态预编译函数预安装到工作区唯一的长期 Context。
     *
     * <p>配置解码阶段调用本方法，将函数声明的首次执行成本移出玩家请求路径。</p>
     *
     * @param function 仅依赖函数参数的预编译函数。
     */
    public static void installPersistent(CompiledScript function) {
        ensureInit();
        ScriptSession session = getPersistentSession();
        synchronized (session) {
            ScriptResult result = session.install(function);
            checkResult("预安装长期预编译脚本函数失败: " + function.getFunctionName(), result);
        }
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
            return invokeCompiled((ScriptSession) session, function, args);
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
        Map<String, Object> variables = createSessionVariables(options.getVariables());
        if (session instanceof ManagedSession) {
            ((ManagedSession) session).rebind(variables, transientBindings);
        } else {
            injectGlobals(session, variables);
        }
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
        if (session instanceof ManagedSession) {
            ((ManagedSession) session).bind(key, value);
        } else {
            injectGlobal(session, key, value);
        }
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
        persistentSession = null;
        persistentArguments = null;
    }

    /** 在配置重载前销毁长期函数 Context，避免保留旧配置的函数定义。 */
    public static synchronized void resetPersistentSession() {
        ScriptSession current = persistentSession;
        if (current == null) {
            return;
        }
        try {
            current.close();
        } catch (Throwable throwable) {
            LOGGER.warning("[Script] 关闭长期脚本会话失败: " + throwable.getMessage());
        }
        persistentSession = null;
        persistentArguments = null;
    }

    /**
     * 预热工作区唯一的长期 Context。
     *
     * <p>在插件启动或配置重载完成后调用，将 Context 创建成本从玩家请求路径移出。</p>
     */
    public static void warmPersistentSession() {
        ensureInit();
        getPersistentSession();
    }

    private static void ensureInit() {
        if (workspace == null) {
            init();
        }
    }

    private static synchronized ScriptSession getPersistentSession() {
        ScriptSession existing = persistentSession;
        if (existing != null) {
            return existing;
        }
        Map<String, Object> bindings = new LinkedHashMap<>();
        bindings.put("stateAPI", StateAPI.INSTANCE);
        for (Map.Entry<String, java.util.function.Function<Object[], Object>> entry : GlobalFunctions.getAll().entrySet()) {
            java.util.function.Function<Object[], Object> function = entry.getValue();
            bindings.put(entry.getKey(), (ScriptFunction) values -> {
                Object[] arguments = unwrapArguments(values);
                return function.apply(arguments);
            });
        }
        Object[] arguments = new Object[CARRIER_SIZE];
        bindings.put(ARGS_GLOBAL, arguments);
        ScriptSession created = workspace.createSession(bindings, java.util.Collections.emptyList());
        persistentSession = created;
        persistentArguments = arguments;
        return created;
    }

    private static Map<String, Object> createScriptVariables(Map<String, Object> variables) {
        Map<String, Object> scriptVariables = new LinkedHashMap<>();
        scriptVariables.put("stateAPI", StateAPI.INSTANCE);
        scriptVariables.putAll(variables);
        return scriptVariables;
    }

    private static Map<String, Object> createSessionVariables(Map<String, Object> variables) {
        return createScriptVariables(variables);
    }

    private static ManagedSession createSession(Map<String, Object> variables, java.util.List<String> preludeScripts) {
        return createSession(variables, preludeScripts, java.util.Collections.emptySet());
    }

    private static ManagedSession createSession(Map<String, Object> variables, java.util.List<String> preludeScripts, Set<String> transientBindings) {
        Map<String, Object> bindings = new LinkedHashMap<>(variables);
        Object[] carrier = new Object[CARRIER_SIZE];
        bindings.put(CARRIER_GLOBAL, carrier);
        for (Map.Entry<String, java.util.function.Function<Object[], Object>> entry : GlobalFunctions.getAll().entrySet()) {
            java.util.function.Function<Object[], Object> function = entry.getValue();
            bindings.put(entry.getKey(), (ScriptFunction) values -> {
                Object[] arguments = unwrapArguments(values);
                return function.apply(arguments);
            });
        }
        SessionTimerHost timerHost = new SessionTimerHost();
        bindings.put("__plannersTimerHost", timerHost);
        ReusableScriptSession delegate = workspace.createReusableSession(
            bindings,
            preludeScripts,
            transientBindings
        );
        ManagedSession session = new ManagedSession(delegate, variables, carrier, timerHost);
        timerHost.attach(session);
        ACTIVE_SESSIONS.add(session);
        session.installTimerFunctions();
        return session;
    }

    /** 将单个键值写入任意会话的 JS 全局作用域。 */
    private static void injectGlobal(ScriptSession session, String key, Object value) {
        injectGlobals(session, java.util.Collections.singletonMap(key, value));
    }

    /**
     * 通过载体数组将一组键值批量写入任意会话的 JS 全局作用域。
     *
     * 载体数组在会话创建时以宿主对象注入，JS 与宿主共享同一引用；
     * 宿主写入元素后用一条 eval 把载体槽位赋给全局变量。
     */
    private static void injectGlobals(ScriptSession session, Map<String, Object> values) {
        if (values.isEmpty()) {
            return;
        }
        Object[] carrier;
        if (session instanceof ManagedSession) {
            carrier = ((ManagedSession) session).carrier();
        } else {
            throw new IllegalArgumentException("会话不支持运行期全局绑定: " + session.getClass().getName());
        }
        synchronized (session) {
            StringBuilder source = new StringBuilder();
            int index = 0;
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                if (index >= CARRIER_SIZE) {
                    throw new IllegalStateException("会话全局绑定数量超出限制: " + values.size());
                }
                carrier[index] = entry.getValue();
                source.append("globalThis[\"").append(escapeJs(entry.getKey())).append("\"] = ")
                    .append(CARRIER_GLOBAL).append('[').append(index).append("];\n");
                index++;
            }
            ScriptResult result = session.eval(source.toString());
            checkResult("写入会话全局绑定失败", result);
        }
    }

    private static String escapeJs(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            switch (ch) {
                case '\\':
                case '"':
                    builder.append('\\').append(ch);
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    builder.append(ch);
            }
        }
        return builder.toString();
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

    /**
     * 会话级定时器宿主：负责向 Bukkit 调度器注册任务，并在触发时回到会话执行 JS 回调。
     *
     * JS 回调本体保存在会话内的 JS 定时器表中，宿主只持有数字任务 ID，
     * 因此不依赖任何非公开的引擎绑定能力。
     */
    private static final class SessionTimerHost {

        private volatile ManagedSession session;

        void attach(ManagedSession owner) {
            this.session = owner;
        }

        public int allocId() {
            return TIMER_ID.getAndIncrement();
        }

        public void schedule(int taskId, long delay, boolean repeating) {
            ManagedSession owner = session;
            if (owner == null) {
                throw new IllegalStateException("Timer host 未绑定会话");
            }
            owner.schedule(taskId, delay, repeating);
        }

        public void cancel(int taskId) {
            ManagedSession owner = session;
            if (owner != null) {
                owner.cancelById(taskId);
            }
        }
    }

    private static final class ManagedSession implements ScriptSession {

        private static final String TIMER_GLUE =
            "globalThis.__plannersTimers = {};\n" +
            "globalThis.setTimeout = function(fn, delay) {\n" +
            "    var id = __plannersTimerHost.allocId();\n" +
            "    __plannersTimers[id] = fn;\n" +
            "    __plannersTimerHost.schedule(id, delay == null ? 0 : delay, false);\n" +
            "    return id;\n" +
            "};\n" +
            "globalThis.setInterval = function(fn, period) {\n" +
            "    var id = __plannersTimerHost.allocId();\n" +
            "    __plannersTimers[id] = fn;\n" +
            "    __plannersTimerHost.schedule(id, period == null ? 1 : period, true);\n" +
            "    return id;\n" +
            "};\n" +
            "globalThis.clearTimer = function(id) {\n" +
            "    delete __plannersTimers[id];\n" +
            "    __plannersTimerHost.cancel(id);\n" +
            "};\n" +
            "globalThis.__plannersTick = function(id, repeating) {\n" +
            "    var fn = __plannersTimers[id];\n" +
            "    if (fn == null) {\n" +
            "        return;\n" +
            "    }\n" +
            "    if (!repeating) {\n" +
            "        delete __plannersTimers[id];\n" +
            "    }\n" +
            "    fn();\n" +
            "};\n";

        private final ReusableScriptSession delegate;
        private final Map<String, Object> variables;
        private final Object[] carrier;
        private final Map<Integer, ScheduledTask> tasks = new ConcurrentHashMap<>();
        private boolean closeRequested;
        private boolean closed;

        private ManagedSession(ReusableScriptSession delegate, Map<String, Object> variables, Object[] carrier, SessionTimerHost timerHost) {
            this.delegate = delegate;
            this.variables = variables;
            this.carrier = carrier;
        }

        Object[] carrier() {
            return carrier;
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

        /** 重绑定会话全局变量并清理上一次声明的临时变量。 */
        private void rebind(Map<String, ? extends Object> nextVariables, Set<String> transientBindings) {
            synchronized (this) {
                if (closed || closeRequested) {
                    throw new IllegalStateException("Session 已关闭或正在关闭，不能重新绑定");
                }
                variables.clear();
                variables.put("stateAPI", StateAPI.INSTANCE);
                variables.putAll(nextVariables);
            }
            Map<String, Object> merged = new LinkedHashMap<>(variables);
            delegate.rebind(merged, transientBindings);
        }

        /** 替换一个临时全局变量。 */
        private void bind(String key, Object value) {
            synchronized (this) {
                if (closed || closeRequested) {
                    throw new IllegalStateException("Session 已关闭或正在关闭，不能绑定变量");
                }
                variables.put(key, value);
            }
            delegate.bind(key, value);
        }

        private void installTimerFunctions() {
            synchronized (this) {
                if (closed || closeRequested) {
                    throw new IllegalStateException("Session 已关闭或正在关闭，不能安装定时器");
                }
                ScriptResult result = delegate.eval(TIMER_GLUE);
                checkResult("安装定时器函数失败", result);
            }
        }

        private void schedule(int taskId, long delay, boolean repeating) {
            synchronized (this) {
                if (closed || closeRequested) {
                    throw new IllegalStateException("Session 已关闭或正在关闭，不能创建定时任务");
                }
            }
            long ticks = Math.max(delay, 0);
            long period = repeating ? Math.max(delay, 1) : -1;
            ScheduledTask scheduledTask = new ScheduledTask();
            tasks.put(taskId, scheduledTask);
            Runnable action = () -> runTask(taskId, repeating);
            try {
                BukkitTask task;
                if (repeating) {
                    task = Bukkit.getScheduler().runTaskTimer(BukkitPlugin.getInstance(), action, ticks, period);
                } else {
                    task = Bukkit.getScheduler().runTaskLater(BukkitPlugin.getInstance(), action, ticks);
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
        }

        private void cancelById(int taskId) {
            ScheduledTask task = tasks.remove(taskId);
            if (task == null) {
                return;
            }
            task.cancel();
            closeIfIdle();
        }

        private void runTask(int taskId, boolean repeating) {
            synchronized (this) {
                if (closed) {
                    return;
                }
            }
            Map<String, Object> previous = ScriptContext.getCurrent();
            ScriptContext.setCurrent(variables);
            try {
                ScriptResult result = delegate.invoke("__plannersTick", taskId, repeating);
                checkResult("定时脚本执行失败", result);
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
