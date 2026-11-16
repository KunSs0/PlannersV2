package com.gitee.planners.module.action.rule

import com.gitee.planners.module.action.ActionArguments
import com.gitee.planners.module.action.ActionTarget
import com.gitee.planners.module.action.query.QueryScope
import com.gitee.planners.module.compat.faction.EntityFriendlyPolicy
import org.bukkit.entity.LivingEntity

/**
 * `team.ally`：只保留与施法者互为友方的目标。
 *
 * 与目标器 `ally` 同一口径：阵营判定统一走 [EntityFriendlyPolicy]，施法者自身算友方，
 * 因此需要排除自己时另接 `include-self` 之外的目标器或在前置容器上单独处理。
 */
object TeamAllyRule : EntityFilterRule() {

    override val id = "team.ally"

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
     * 目标存活且与施法者同阵营时保留。
     *
     * @param scope 当前执行上下文，施法者取 `sender`。
     * @param target 候选目标。
     * @param entity 目标对应的 Bukkit 实体。
     * @param arguments 规则参数。
     * @param compiled 编译结果。
     * @return 互为友方时返回 true。
     */
    override fun matches(
        scope: QueryScope,
        target: ActionTarget,
        entity: LivingEntity,
        arguments: ActionArguments,
        compiled: Any?,
    ): Boolean {
        val source = scope.sender
        if (source !is LivingEntity) {
            return false
        }
        if (!target.isActive()) {
            return false
        }
        return EntityFriendlyPolicy.isFriendly(source, entity)
    }
}
