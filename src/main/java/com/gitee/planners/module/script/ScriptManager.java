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
import com.gitee.planners.module.script.api.StateAPI;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 脚本管理器（静态门面）。
 *
 * 内部通过 ScriptEngine 的 GraalJS 实现执行 JavaScript。
 */
public final class ScriptManager {

    private static final Logger LOGGER = Logger.getLogger("Script");
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
            java.util.Collections.emptyList(),
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
        try {
            com.gitee.scriptengine.api.ScriptContext context = workspace.createContext(variables);
            installGlobalFunctions(context);
            try {
                ScriptResult result = context.eval(source);
                checkResult("执行脚本失败", result);
                return result.getValue();
            } finally {
                context.close();
            }
        } finally {
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
        com.gitee.scriptengine.api.ScriptContext context = workspace.createContext(createSessionVariables(options.getVariables()));
        installGlobalFunctions(context);
        return new ScriptSessionImpl(context);
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
        ScriptSession session = null;
        try {
            com.gitee.scriptengine.api.ScriptContext context = workspace.createContext(createSessionVariables(variables));
            installGlobalFunctions(context);
            session = new ScriptSessionImpl(context);
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

    private static void installGlobalFunctions(com.gitee.scriptengine.api.ScriptContext context) {
        for (Map.Entry<String, java.util.function.Function<Object[], Object>> entry : GlobalFunctions.getAll().entrySet()) {
            java.util.function.Function<Object[], Object> function = entry.getValue();
            context.getBindings().putMember(entry.getKey(), (ScriptFunction) values -> {
                Object[] arguments = unwrapArguments(values);
                return function.apply(arguments);
            });
        }
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
}
