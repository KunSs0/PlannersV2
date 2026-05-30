package com.gitee.planners.core.condition

import com.gitee.planners.Planners
import com.gitee.planners.module.script.ScriptOptions
import com.gitee.planners.module.script.ScriptManager
import com.gitee.planners.module.script.bridge.PlayerBridge
import org.bukkit.entity.Player

/**
 * 条件执行器。
 * 集中定义在 config.yml 的条件模板通过 key 引用 + 传参覆盖后，由本类执行。
 */
class ConditionEvaluator {

    data class VerifyResult(
        val passed: Boolean,
        val hints: List<String>
    ) {
        companion object {
            val PASSED = VerifyResult(true, emptyList())
        }
    }

    /**
     * 校验条件组。
     *
     * @param conditions 条件引用：key → override props
     * @param player     Bukkit Player
     * @param contextVars 额外上下文变量（供 props 公式引用）
     */
    fun verify(
        conditions: Map<String, Map<String, Any>>,
        player: Player,
        contextVars: Map<String, Any> = emptyMap()
    ): VerifyResult {
        val bridge = PlayerBridge(player)
        val hints = mutableListOf<String>()

        for ((key, overrideProps) in conditions) {
            val cfg = Planners.conditions.get()[key]
            if (cfg == null) {
                error("Unknown condition key: $key")
            }
            val props = resolveProps(cfg.props, overrideProps, bridge, contextVars)

            val options = ScriptOptions.of()
                .set("player", bridge)
                .set("props", props)

            val passed = try {
                ScriptManager.eval(cfg.exper, options) == true
            } catch (e: Exception) {
                false
            }

            if (!passed) {
                hints.add(interpolate(cfg.hint, props))
                return VerifyResult(false, hints)
            }
        }
        return VerifyResult.PASSED
    }

    /**
     * 执行消耗（校验通过后调用）。
     *
     * @param conditions 条件引用：key → override props
     * @param player     Bukkit Player
     * @param contextVars 额外上下文变量（供 props 公式引用）
     */
    fun consume(
        conditions: Map<String, Map<String, Any>>,
        player: Player,
        contextVars: Map<String, Any> = emptyMap()
    ) {
        val bridge = PlayerBridge(player)

        for ((key, overrideProps) in conditions) {
            val cfg = Planners.conditions.get()[key]
            if (cfg == null) {
                error("Unknown condition key: $key")
            }
            if (cfg.consume == null) {
                continue
            }

            val props = resolveProps(cfg.props, overrideProps, bridge, contextVars)

            val options = ScriptOptions.of()
                .set("player", bridge)
                .set("props", props)

            try {
                ScriptManager.eval(cfg.consume, options)
            } catch (e: Exception) {
                // consume 失败不阻塞后续
                e.printStackTrace()
            }
        }
    }

    // ---- 内部 ----

    /**
     * 合并默认 props 与调用处覆盖值，并将 String 值 eval 为实际值。
     */
    private fun resolveProps(
        defaultProps: Map<String, Any>,
        overrideProps: Map<String, Any>,
        bridge: PlayerBridge,
        contextVars: Map<String, Any>
    ): Map<String, Any> {
        val merged = defaultProps.toMutableMap()
        merged.putAll(overrideProps)

        val resolved = LinkedHashMap<String, Any>()
        for ((k, v) in merged) {
            resolved[k] = when (v) {
                is String -> evalValue(v, bridge, contextVars)
                else -> v
            }
        }
        return resolved
    }

    /**
     * 对 String 值尝试求值：
     * 1. 纯数字 → 转为 Int/Double
     * 2. JS 公式 → 执行并返回结果
     * 3. eval 抛异常 → 作为字面量字符串
     */
    private fun evalValue(
        expr: String,
        bridge: PlayerBridge,
        contextVars: Map<String, Any>
    ): Any {
        // 纯数字字符串
        val doubleValue = expr.toDoubleOrNull()
        if (doubleValue != null) {
            if (doubleValue % 1 == 0.0) {
                return doubleValue.toInt()
            } else {
                return doubleValue
            }
        }
        // JS 公式
        return try {
            val options = ScriptOptions.of().set("player", bridge)
            contextVars.forEach { (k, v) -> options.set(k, v) }
            ScriptManager.eval(expr, options) ?: expr
        } catch (e: Exception) {
            expr
        }
    }

    /**
     * hint 中的 {props.xxx} 替换为实际值。
     */
    private fun interpolate(hint: String, props: Map<String, Any>): String {
        var result = hint
        for ((key, value) in props) {
            result = result.replace("{props.$key}", value.toString())
        }
        return result
    }
}
