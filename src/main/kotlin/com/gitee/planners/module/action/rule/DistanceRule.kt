package com.gitee.planners.module.action.rule

import com.gitee.planners.module.action.ActionArguments
import com.gitee.planners.module.action.ActionTarget
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.LivingEntity

/**
 * `distance`：按与施法者的直线距离过滤。
 *
 * 距离取施法者与目标实体当前位置的三维距离，跨世界直接判为不通过；
 * `max` / `min` 至少写一个，端点均为闭区间（`max=10` 表示 10 格以内，含 10 格）。
 * 与 `@range{radius}` 的区别：选区负责「谁在范围内」，本规则负责「已经选出来的目标
 * 离施法者多远」，因此可以叠在 `@online-players`、`@target.finder` 这类非选区容器上。
 *
 * 施法者取 `caster`（未单独指定时等于 `sender`），因此在逐目标子流程里仍然是本次施法的发起者。
 */
object DistanceRule : EntityFilterRule() {

    override val id = "distance"

    /**
     * 校验区间参数。
     *
     * 动态取值（`<skill.var.距离>`）留到求值阶段校验，这里只校验能读到的字面量。
     *
     * @param arguments 规则参数。
     * @throws IllegalArgumentException 缺少区间、出现未知参数或字面量为负时抛出。
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
     * 距离落在区间内时保留。
     *
     * @param scope 当前执行上下文。
     * @param target 候选目标。
     * @param entity 目标对应的 Bukkit 实体。
     * @param arguments 规则参数。
     * @param compiled 编译结果。
     * @return 距离落在 `[min, max]` 内时返回 true。
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
        val source = resolveSource(scope)
        if (source == null) {
            return false
        }
        // 跨世界没有可比较的距离，直接判为不通过，避免 distance 抛异常打断流程。
        if (source.world != entity.world) {
            return false
        }
        val squared = source.location.distanceSquared(entity.location)
        val max = bound(scope, arguments, "max")
        if (max != null && squared > max * max) {
            return false
        }
        val min = bound(scope, arguments, "min")
        if (min != null && squared < min * min) {
            return false
        }
        return true
    }

    /**
     * 取距离基准实体。
     *
     * @param scope 当前执行上下文。
     * @return 施法者；不是生物时返回 null。
     */
    private fun resolveSource(scope: QueryScope): LivingEntity? {
        val caster = scope.caster
        if (caster is LivingEntity) {
            return caster
        }
        val sender = scope.sender
        if (sender is LivingEntity) {
            return sender
        }
        return null
    }

    /**
     * 读取字面量端点，用于加载期校验。
     *
     * @param arguments 规则参数。
     * @param key 参数名。
     * @return 端点值；未配置或不是字面量时返回 null。
     * @throws IllegalArgumentException 字面量不是数值或为负数时抛出。
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
     * 读取运行时端点。
     *
     * @param scope 当前执行上下文。
     * @param arguments 规则参数。
     * @param key 参数名。
     * @return 端点值；未配置时返回 null。
     */
    private fun bound(scope: QueryScope, arguments: ActionArguments, key: String): Double? {
        if (!arguments.contains(key)) {
            return null
        }
        val value = arguments.requireNumber(scope, key)
        checkBound(value, key, arguments.path)
        return value
    }

    /**
     * 校验端点非负。
     *
     * @param value 端点值。
     * @param key 参数名。
     * @param path 配置路径。
     */
    private fun checkBound(value: Double, key: String, path: String) {
        if (!value.isFinite() || value < 0.0) {
            throw IllegalArgumentException("$path $key 必须为非负有限值。")
        }
    }
}
