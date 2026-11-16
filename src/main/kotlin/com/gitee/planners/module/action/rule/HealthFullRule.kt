package com.gitee.planners.module.action.rule

import com.gitee.planners.module.action.ActionArguments
import com.gitee.planners.module.action.ActionTarget
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.LivingEntity

/**
 * `health.full`：只保留满血目标。
 *
 * 判据是 `当前生命 >= 最大生命`。需要“排除满血”时不要给本规则加开关，
 * 直接用 [HealthDamagedRule]，避免同一判据长出成对的反向规则。
 */
object HealthFullRule : EntityFilterRule() {

    override val id = "health.full"

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
     * 目标存活且生命达到上限时保留。
     *
     * @param scope 当前执行上下文。
     * @param target 候选目标。
     * @param entity 目标对应的 Bukkit 实体。
     * @param arguments 规则参数。
     * @param compiled 编译结果。
     * @return 满血时返回 true。
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
        return entity.health >= maximum
    }
}
