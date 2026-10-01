package com.gitee.planners.core.skill.condition

import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.context.SkillExecutionContext
import org.bukkit.entity.Player

interface SkillCastCondition {
    fun verify(
        player: Player,
        skill: PlayerSkill,
        execution: SkillExecutionContext,
        props: Map<String, Any>
    ): String?

    fun prepareConsume(
        player: Player,
        skill: PlayerSkill,
        execution: SkillExecutionContext,
        props: Map<String, Any>
    ): CastCostReservation?
}
