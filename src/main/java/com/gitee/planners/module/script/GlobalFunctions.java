package com.gitee.planners.module.script;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局函数注册表
 * <p>
 * 在引擎初始化前注册的函数会在 {@link ScriptManager#init()} 时统一注入。
 * 初始化后注册的函数会立即注入到当前引擎。
 */
public final class GlobalFunctions {

    private static final Map<String, JsFunction> FUNCTIONS = new ConcurrentHashMap<>();

    private GlobalFunctions() {}

    /**
     * 注册全局函数
     */
    public static void register(String name, JsFunction function) {
        FUNCTIONS.put(name, function);
    }

    /**
     * 将所有已注册的函数注入到引擎
     */
    static void applyTo(JsEngine engine) {
        FUNCTIONS.forEach(engine::registerFunction);
    }

    /**
     * 获取所有已注册的函数 (只读)
     */
    public static Map<String, JsFunction> getAll() {
        return FUNCTIONS;
    }
}
