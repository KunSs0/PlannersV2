package com.gitee.planners.core.skill.directing

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.directing.DirectingCandidateFilter
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.skilltree.SkillTreeNodeEffectService
import com.gitee.planners.module.action.ActionScopeFactory
import com.gitee.planners.module.action.ActionTargeterStep
import org.bukkit.entity.Player

/**
 * 把技能上声明的过滤链编译成一次会话可用的过滤器。
 *
 * 过滤链需要施法者与技能等级才能求值，因此每个会话各自编译一份，不跨玩家或跨次施法共享。
 */
object DirectingFilterCompiler {

    /**
     * 为一次指向会话编译过滤器。
     *
     * @param player 当前施法者。
     * @param skill 当前技能定义。
     * @param steps 技能上声明的过滤链。
     * @return 过滤器；未声明过滤链时返回 null，表示只做几何校验。
     */
    fun compile(player: Player, skill: ImmutableSkill, steps: List<ActionTargeterStep>): DirectingCandidateFilter? {
        if (steps.isEmpty()) {
            return null
        }
        // 技能等级用于 `<skill.level>` 这类参数；档案未加载时按 0 级求值。
        val level = SkillTreeNodeEffectService.getSkillLevel(player.plannersTemplate, skill.id)
        val scope = ActionScopeFactory.forPlayer(
            player,
            skill,
            level,
            emptyMap(),
            null,
            "skill:" + skill.id + ".directing.filter",
        )
        return DirectingTargeterFilter(scope, steps)
    }
}
