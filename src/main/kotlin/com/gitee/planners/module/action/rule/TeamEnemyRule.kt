package com.gitee.planners.module.action.rule

import com.gitee.planners.module.action.ActionArguments
import com.gitee.planners.module.action.ActionTarget
import com.gitee.planners.module.action.query.QueryScope
import com.gitee.planners.module.compat.faction.EntityFriendlyPolicy
import org.bukkit.entity.LivingEntity

/**
 * `team.enemy`：只保留与施法者敌对的目标。
 *
 * 与目标器 `enemy` 同一口径：排除施法者自身，阵营判定统一走 [EntityFriendlyPolicy]。
 */
object TeamEnemyRule : EntityFilterRule() {

    override val id = "team.enemy"

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
     * 目标存活、不是施法者本人且与施法者敌对时保留。
     *
     * @param scope 当前执行上下文，施法者取 `sender`。
     * @param target 候选目标。
     * @param entity 目标对应的 Bukkit 实体。
     * @param arguments 规则参数。
     * @param compiled 编译结果。
     * @return 敌对时返回 true。
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
        if (entity.uniqueId == source.uniqueId) {
            return false
        }
        return !EntityFriendlyPolicy.isFriendly(source, entity)
    }
}
