package com.gitee.planners.module.action.rule

import com.gitee.planners.module.action.ActionArguments
import com.gitee.planners.module.action.ActionTarget
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.LivingEntity

/**
 * `health.damaged`：只保留严格缺血的目标。
 *
 * 判据是 `0 < 当前生命 < 最大生命`：满血与死亡都不保留。
 * 与 `wounded{ratio=1.0}` 的区别是上界**不含**满血，因此不需要 0.999 这类近似阈值。
 */
object HealthDamagedRule : EntityFilterRule() {

    override val id = "health.damaged"

    /**
     * 规则没有参数。
     *
     * @param arguments 规则参数。
     * @throws IllegalArgumentException 出现未知参数时抛出。
     */
    override fun validate(arguments: ActionArguments) {
        arguments.rejectUnknown()
    }

    /**
     * 目标存活且生命严格低于上限时保留。
     *
     * @param scope 当前执行上下文。
     * @param target 候选目标。
     * @param entity 目标对应的 Bukkit 实体。
     * @param arguments 规则参数。
     * @param compiled 编译结果。
     * @return 严格缺血时返回 true。
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
        if (entity.health <= 0.0) {
            return false
        }
        return entity.health < maximum
    }
}
