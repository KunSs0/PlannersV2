package com.gitee.planners.module.script.functions;

import com.gitee.planners.api.job.Variable;
import com.gitee.planners.core.config.ImmutableSkill;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;
import com.gitee.planners.module.script.ScriptContext;
import com.gitee.planners.module.script.ScriptOptions;

import java.util.Map;

/**
 * 变量计算函数
 * <pre>{@code
 * // JS: calcVar("power")  — 计算当前技能的 power 变量（每次重新计算）
 * // JS: getVar("power")   — 获取当前技能的 power 变量（首次计算后缓存）
 * }</pre>
 */
public final class VariableFunctions {

    private VariableFunctions() {}

    public static void register() {
        // calcVar(key) — 每次重新计算变量表达式
        GlobalFunctions.register("calcVar", args -> {
            String key = ScriptArgs.getString(args, 0);
            if (key == null) {
                return null;
            }
            return resolveVar(key);
        });

        // getVar(key) — 带缓存，同一次脚本执行内只计算一次
        GlobalFunctions.register("getVar", args -> {
            String key = ScriptArgs.getString(args, 0);
            if (key == null) {
                return null;
            }
            if (ScriptContext.hasVarCache(key)) {
                return ScriptContext.getVarCache(key);
            }
            Object value = resolveVar(key);
            ScriptContext.putVarCache(key, value);
            return value;
        });
    }

    /**
     * 从当前脚本上下文中解析并计算技能变量
     */
    private static Object resolveVar(String key) {
        Map<String, Object> ctx = ScriptContext.getCurrent();
        if (ctx == null) {
            return null;
        }
        Object skillObj = ctx.get("skill");
        if (!(skillObj instanceof ImmutableSkill)) {
            return null;
        }
        ImmutableSkill skill = (ImmutableSkill) skillObj;
        Variable var = skill.getVariableOrNull(key);
        if (var == null) {
            return null;
        }
        ScriptOptions options = ScriptOptions.of();
        for (Map.Entry<String, Object> entry : ctx.entrySet()) {
            options.set(entry.getKey(), entry.getValue());
        }
        return var.run(options).join();
    }
}
