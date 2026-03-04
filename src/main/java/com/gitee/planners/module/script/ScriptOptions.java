package com.gitee.planners.module.script;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 脚本执行选项
 * <p>
 * 包含上下文变量和执行配置 (如 async 标志)。
 */
public class ScriptOptions {

    private final Map<String, Object> variables = new LinkedHashMap<>();
    private boolean async = false;

    public ScriptOptions set(String key, Object value) {
        variables.put(key, value);
        return this;
    }

    public ScriptOptions async(boolean async) {
        this.async = async;
        return this;
    }

    public boolean isAsync() {
        return async;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    // ---- 静态工厂 ----

    public static ScriptOptions of() {
        return new ScriptOptions();
    }

    /**
     * 通用选项 (注入 sender)
     */
    public static ScriptOptions common(Object sender) {
        return new ScriptOptions().set("sender", sender);
    }

    /**
     * 复制并替换 sender
     */
    public static ScriptOptions sender(Object sender, ScriptOptions base) {
        ScriptOptions options = new ScriptOptions();
        options.variables.putAll(base.variables);
        options.async = base.async;
        options.set("sender", sender);
        return options;
    }
}
