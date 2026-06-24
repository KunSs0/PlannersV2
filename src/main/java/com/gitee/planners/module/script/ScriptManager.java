package com.gitee.planners.module.script;

import com.gitee.scriptengine.api.ScriptSession;
import com.gitee.scriptengine.core.JsEngine;
import com.gitee.scriptengine.core.JsEngineFactory;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 脚本管理器（静态门面）。
 *
 * 内部通过 Script Engine 的 JsEngineFactory 自动选择 Nashorn（Java 8-14）或 GraalJS（Java 17+）。
 */
public final class ScriptManager {

    private static final Logger LOGGER = Logger.getLogger("Script");
    private static JsEngine engine;

    private ScriptManager() {}

    /**
     * 初始化引擎（插件启动时调用）。
     */
    public static void init() {
        if (engine != null) {
            return;
        }
        // 注册所有全局函数到本地注册表
        ScriptFunctionRegistry.registerAll();

        // 通过 Script Engine 自动选择引擎
        File scriptDir = new File("plugins/Planners/scripts");
        engine = JsEngineFactory.INSTANCE.create(scriptDir);

        // 将本地注册的全局函数注入引擎
        GlobalFunctions.getAll().forEach((name, fn) ->
            engine.registerFunction(name, fn::apply)
        );
        LOGGER.info("[Script] 引擎初始化完成: " + engine.name());
    }

    /**
     * 执行脚本。
     *
     * 保存并恢复调用方的 ScriptContext，避免嵌套调用清掉外层上下文。
     */
    public static Object eval(String source, ScriptOptions options) {
        ensureInit();
        Map<String, Object> previous = ScriptContext.getCurrent();
        Map<String, Object> variables = options.getVariables();
        ScriptContext.setCurrent(variables);
        try {
            return engine.eval(source, variables);
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
        return engine.openSession(options.getVariables());
    }

    /**
     * 获取当前引擎。
     */
    public static JsEngine getEngine() {
        ensureInit();
        return engine;
    }

    /**
     * 关闭引擎（插件卸载时调用）。
     */
    public static void shutdown() {
        if (engine != null) {
            engine.close();
            engine = null;
        }
    }

    private static void ensureInit() {
        if (engine == null) {
            init();
        }
    }
}
