package com.gitee.planners.core.skill.directing

import com.gitee.planners.api.directing.DirectingCandidateFilter
import com.gitee.planners.module.action.ActionTarget
import com.gitee.planners.module.action.ActionTargeterStep
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * 由目标器链实现的候选过滤器。
 *
 * 每个候选都先经过目标器链：容器被过滤为空即视为拒绝。链上的目标器语义与动作行完全一致，
 * 因此 `@alive` / `@ally` 这类判定只有一份实现，预览环与服务端确认也共用同一份结果。
 *
 * @property scope 绑定施法者与技能的查询上下文。
 * @property steps 按声明顺序执行的目标器步骤。
 */
class DirectingTargeterFilter(
    private val scope: QueryScope,
    private val steps: List<ActionTargeterStep>,
) : DirectingCandidateFilter {

    /**
     * 对单个候选执行过滤链。
     *
     * @param entity 候选实体。
     * @return 链上所有步骤都保留该候选时返回 true。
     */
    override fun accept(entity: Entity): Boolean {
        if (!entity.isValid || entity.isDead) {
            return false
        }
        if (entity !is LivingEntity) {
            return false
        }
        var container: List<ActionTarget> = listOf(toTarget(entity))
        for (step in steps) {
            container = step.apply(scope, container)
            if (container.isEmpty()) {
                return false
            }
        }
        return true
    }

    /**
     * 按实体类型包装成动作目标。
     *
     * @param entity 已确认存活的目标实体。
     * @return 玩家目标或普通实体目标。
     */
    private fun toTarget(entity: LivingEntity): ActionTarget {
        if (entity is Player) {
            return ActionTarget.PlayerTarget(entity)
        }
        return ActionTarget.EntityTarget(entity)
    }
}
