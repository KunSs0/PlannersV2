package com.gitee.planners.module.action.rule

import com.gitee.planners.module.action.ActionArguments
import com.gitee.planners.module.action.ActionTarget
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.LivingEntity

/**
 * `health.alive`：只保留仍然存活的目标。
 *
 * 与目标器 `alive` 同一口径，直接读目标自身的存活状态，不额外读生命值，
 * 便于把现有 `@alive` 迁成规则时保持行为不变。
 */
object HealthAliveRule : EntityFilterRule() {

    override val id = "health.alive"

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
     * 目标仍然存活时保留。
     *
     * @param scope 当前执行上下文。
     * @param target 候选目标。
     * @param entity 目标对应的 Bukkit 实体。
     * @param arguments 规则参数。
     * @param compiled 编译结果。
     * @return 存活时返回 true。
     */
    override fun matches(
        scope: QueryScope,
        target: ActionTarget,
        entity: LivingEntity,
        arguments: ActionArguments,
        compiled: Any?,
    ): Boolean {
        return target.isActive()
    }
}
