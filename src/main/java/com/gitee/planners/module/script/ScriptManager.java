package com.gitee.planners.module.script;

import com.gitee.planners.module.script.graaljs.GraalJsEngine;
import com.gitee.planners.module.script.nashorn.NashornEngine;

import java.util.Map;
import java.util.logging.Logger;

/**
 * 脚本管理器 (静态门面)
 * <p>
 * 运行时自动选择最佳 JS 引擎:
 * <ul>
 *   <li>Java 8~14: Nashorn</li>
 *   <li>Java 17+: GraalJS (后续实现)</li>
 * </ul>
 */
public final class ScriptManager {

    private static final Logger LOGGER = Logger.getLogger("Script");
    private static volatile JsEngine engine;

    private ScriptManager() {}

    /**
     * 初始化引擎 (插件启动时调用)
     */
    public static void init() {
        if (engine != null) return;
        // 先注册所有函数到 GlobalFunctions
        ScriptFunctionRegistry.registerAll();
        engine = createEngine();
        // 注入已注册的全局函数到引擎
        GlobalFunctions.applyTo(engine);
        LOGGER.info("[Script] 引擎初始化完成: " + engine.name());
    }

    /**
     * 执行脚本
     */
    public static Object eval(String source, ScriptOptions options) {
        ensureInit();
        Map<String, Object> variables = options.getVariables();
        ScriptContext.setCurrent(variables);
        try {
            return engine.eval(source, variables);
        } finally {
            ScriptContext.clear();
        }
    }

    /**
     * 执行脚本 (无上下文变量)
     */
    public static Object eval(String source) {
        return eval(source, new ScriptOptions());
    }

    /**
     * 打开会话 (状态回调等跨调用场景)
     */
    public static JsSession openSession(ScriptOptions options) {
        ensureInit();
        return engine.openSession(options.getVariables());
    }

    /**
     * 获取当前引擎
     */
    public static JsEngine getEngine() {
        ensureInit();
        return engine;
    }

    /**
     * 关闭引擎 (插件卸载时调用)
     */
    public static void shutdown() {
        if (engine != null) {
            engine.close();
            engine = null;
        }
    }

    private static JsEngine createEngine() {
        int version = getMajorJavaVersion();

        // Java 17+: GraalJS
        if (version >= 17) {
            try {
                return new GraalJsEngine();
            } catch (Throwable e) {
                LOGGER.warning("[Script] GraalJS 不可用，回退到 Nashorn: " + e.getMessage());
            }
        }

        // Java 8~14: Nashorn
        return new NashornEngine();
    }

    private static int getMajorJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.substring(2, 3));
        }
        int dot = version.indexOf('.');
        return Integer.parseInt(dot > 0 ? version.substring(0, dot) : version);
    }

    private static void ensureInit() {
        if (engine == null) init();
    }
}
