package com.gitee.planners.module.script;

import java.util.HashMap;
import java.util.Map;

/**
 * 脚本执行上下文 (ThreadLocal)
 * <p>
 * 在 {@link ScriptManager#eval} 执行期间设置当前上下文变量，
 * 使全局函数内部可通过 {@link #getSender()} 等方法访问执行环境。
 */
public final class ScriptContext {

    private static final ThreadLocal<Map<String, Object>> CURRENT = new ThreadLocal<>();
    /** 同一次脚本执行内的变量缓存 */
    private static final ThreadLocal<Map<String, Object>> VAR_CACHE = new ThreadLocal<>();

    private ScriptContext() {}

    public static void setCurrent(Map<String, Object> variables) {
        CURRENT.set(variables);
        VAR_CACHE.set(new HashMap<>());
    }

    public static Map<String, Object> getCurrent() {
        return CURRENT.get();
    }

    /**
     * 获取变量缓存，未命中返回 null
     */
    public static Object getVarCache(String key) {
        Map<String, Object> cache = VAR_CACHE.get();
        if (cache == null) {
            return null;
        }
        return cache.get(key);
    }

    /**
     * 写入变量缓存
     */
    public static void putVarCache(String key, Object value) {
        Map<String, Object> cache = VAR_CACHE.get();
        if (cache != null) {
            cache.put(key, value);
        }
    }

    /**
     * 检查变量缓存是否存在
     */
    public static boolean hasVarCache(String key) {
        Map<String, Object> cache = VAR_CACHE.get();
        return cache != null && cache.containsKey(key);
    }

    public static void clear() {
        CURRENT.remove();
        VAR_CACHE.remove();
    }

    public static Object getSender() {
        Map<String, Object> ctx = CURRENT.get();
        return ctx != null ? ctx.get("sender") : null;
    }
}
