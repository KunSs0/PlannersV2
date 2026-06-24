package com.gitee.planners.module.script;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 全局函数注册表（纯数据容器，不依赖任何引擎实现）。
 */
public final class GlobalFunctions {

    private static final Map<String, Function<Object[], Object>> FUNCTIONS = new LinkedHashMap<>();

    private GlobalFunctions() {}

    /**
     * 注册全局函数。
     *
     * @param name 函数名，JS 侧直接调用。
     * @param function 函数实现，接收 Object[] 参数，返回结果。
     */
    public static void register(String name, Function<Object[], Object> function) {
        FUNCTIONS.put(name, function);
    }

    /**
     * @return 所有已注册函数的只读副本。
     */
    public static Map<String, Function<Object[], Object>> getAll() {
        return new LinkedHashMap<>(FUNCTIONS);
    }
}
