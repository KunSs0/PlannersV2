package com.gitee.planners.module.action.rule

import com.gitee.planners.module.action.ActionArguments
import com.gitee.planners.module.action.ActionTarget
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.LivingEntity

/**
 * `health.ratio`：按生命比例区间过滤，端点均为闭区间。
 *
 * `min` / `max` 至少写一个，取值在 `(0, 1]`：`max=0.5` 表示生命比例不高于 50%，
 * 与旧 `wounded{ratio}` 的上界口径一致；`min` 用于「缺血但不濒死」这类区间需求。
 * 需要「只要有伤」而不是固定阈值时用 [HealthDamagedRule]，不要写 `max=1.0` 近似。
 */
object HealthRatioRule : EntityFilterRule() {

    override val id = "health.ratio"

    /**
     * 校验区间参数。
     *
     * 动态取值（`<skill.var.阈值>`）在求值阶段校验，这里只校验能读到的字面量。
     *
     * @param arguments 规则参数。
     * @throws IllegalArgumentException 缺少区间、出现未知参数或字面量越界时抛出。
     */
    override fun validate(arguments: ActionArguments) {
        arguments.rejectUnknown("min", "max")
        if (!arguments.contains("min") && !arguments.contains("max")) {
            throw IllegalArgumentException("${arguments.path} 需要 min 或 max 至少一项。")
        }
        val min = literalBound(arguments, "min")
        val max = literalBound(arguments, "max")
        if (min != null && max != null && min > max) {
            throw IllegalArgumentException("${arguments.path} min 不能大于 max。")
        }
    }

    /**
     * 生命比例落在区间内时保留。
     *
     * @param scope 当前执行上下文。
     * @param target 候选目标。
     * @param entity 目标对应的 Bukkit 实体。
     * @param arguments 规则参数。
     * @param compiled 编译结果。
     * @return 比例落在 `[min, max]` 内时返回 true。
     */
    override fun matches(
        scope: QueryScope,
        target: ActionTarget,
        entity: LivingEntity,
        arguments: ActionArguments,
        compiled: Any?,
    ): Boolean {
        if (!target.isActive()) {
            return false
        }
        val maximum = entity.maxHealth
        if (maximum <= 0.0) {
            return false
        }
        val ratio = entity.health / maximum
        val min = ratioBound(scope, arguments, "min")
        if (min != null && ratio < min) {
            return false
        }
        val max = ratioBound(scope, arguments, "max")
        if (max != null && ratio > max) {
            return false
        }
        return true
    }

    /**
     * 读取字面量区间端点，用于加载期校验。
     *
     * @param arguments 规则参数。
     * @param key 参数名。
     * @return 端点值；未配置或不是字面量时返回 null。
     * @throws IllegalArgumentException 字面量不是数值或超出 `(0, 1]` 时抛出。
     */
    private fun literalBound(arguments: ActionArguments, key: String): Double? {
        val text = arguments.literalText(key)
        if (text == null) {
            return null
        }
        val value = text.trim().toDoubleOrNull()
        if (value == null) {
            throw IllegalArgumentException("${arguments.path} $key must be a number.")
        }
        checkBound(value, key, arguments.path)
        return value
    }

    /**
     * 读取运行时区间端点。
     *
     * @param scope 当前执行上下文。
     * @param arguments 规则参数。
     * @param key 参数名。
     * @return 端点值；未配置时返回 null。
     */
    private fun ratioBound(scope: QueryScope, arguments: ActionArguments, key: String): Double? {
        if (!arguments.contains(key)) {
            return null
        }
        val value = arguments.requireNumber(scope, key)
        checkBound(value, key, arguments.path)
        return value
    }

    /**
     * 校验端点落在 `(0, 1]`。
     *
     * @param value 端点值。
     * @param key 参数名。
     * @param path 配置路径。
     */
    private fun checkBound(value: Double, key: String, path: String) {
        if (value <= 0.0 || value > 1.0) {
            throw IllegalArgumentException("$path $key 必须在 (0, 1] 区间。")
        }
    }
}
