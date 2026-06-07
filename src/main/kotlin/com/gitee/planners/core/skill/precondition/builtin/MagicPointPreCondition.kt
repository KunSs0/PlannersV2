package com.gitee.planners.core.skill.precondition.builtin

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.player.magic.MagicPointProvider.Companion.magicPoint
import com.gitee.planners.core.skill.precondition.CastPreCondition
import com.gitee.planners.core.skill.precondition.CastPreConditionResult
import com.gitee.planners.module.script.ScriptOptions
import org.bukkit.entity.Player
import taboolib.common5.cint
import taboolib.platform.util.asLangText

/**
 * 魔法值检查。
 */
class MagicPointPreCondition : CastPreCondition {

    override val key = "mp"

    override val priority = 200

    override fun hint(player: Player, failure: CastPreConditionResult.Failure): String {
        return player.asLangText("precondition-mp")
    }

    override fun verify(player: Player, skill: PlayerSkill, options: ScriptOptions): CastPreConditionResult? {
        val cost = skill.getVariableOrNull("mp")?.run(options)?.getNow(null)?.cint
        if (cost == null) {
            return null
        }
        val current = player.plannersTemplate.magicPoint
        if (cost > current) {
            return CastPreConditionResult.Failure(this, mapOf(
                "required" to cost,
                "current" to current
            ))
        }
        return null
    }

    override fun consume(player: Player, skill: PlayerSkill, options: ScriptOptions) {
        val cost = skill.getVariableOrNull("mp")?.run(options)?.getNow(null)?.cint
        if (cost != null) {
            player.plannersTemplate.magicPoint -= cost
        }
    }

}
