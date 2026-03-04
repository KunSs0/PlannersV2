package com.gitee.planners.module.script;

import java.util.Map;

/**
 * 脚本执行上下文 (ThreadLocal)
 * <p>
 * 在 {@link ScriptManager#eval} 执行期间设置当前上下文变量，
 * 使全局函数内部可通过 {@link #getSender()} 等方法访问执行环境。
 */
public final class ScriptContext {

    private static final ThreadLocal<Map<String, Object>> CURRENT = new ThreadLocal<>();

    private ScriptContext() {}

    public static void setCurrent(Map<String, Object> variables) {
        CURRENT.set(variables);
    }

    public static Map<String, Object> getCurrent() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static Object getSender() {
        Map<String, Object> ctx = CURRENT.get();
        return ctx != null ? ctx.get("sender") : null;
    }
}
