package com.gitee.planners.core.skill.condition

import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.context.SkillExecutionContext
import org.bukkit.entity.Player

interface SkillCastCondition {

    /**
     * 加载期校验条件参数。
     *
     * 只校验参数自身的形状与引用可用性；默认为空实现，需要参数的条重视具体实现覆盖。
     *
     * @param props 条件参数。
     * @param path 配置路径，用于错误信息。
     */
    fun validate(props: Map<String, Any>, path: String) {
    }

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
