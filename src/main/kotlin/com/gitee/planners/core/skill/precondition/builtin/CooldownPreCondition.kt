package com.gitee.planners.core.skill.precondition.builtin

import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.cooler.Cooler
import com.gitee.planners.core.skill.precondition.CastPreCondition
import com.gitee.planners.core.skill.precondition.CastPreConditionResult
import com.gitee.planners.module.script.ScriptOptions
import org.bukkit.entity.Player
import taboolib.common5.cint

/**
 * 冷却检查。
 *
 * 校验技能是否处于冷却中，通过后在 consume 阶段设置冷却。
 * 从技能变量 "cooldown" 读取冷却时间（ticks）。
 */
class CooldownPreCondition : CastPreCondition {

    override val key = "cooldown"

    override val priority = 100

    override val hint = "技能冷却中"

    override fun verify(player: Player, skill: PlayerSkill, options: ScriptOptions): CastPreConditionResult? {
        val remaining = Cooler.INSTANCE.get(player, skill)
        if (remaining > 0L) {
            return CastPreConditionResult.Failure(this, mapOf("remaining" to remaining))
        }
        return null
    }

    override fun consume(player: Player, skill: PlayerSkill, options: ScriptOptions) {
        val cooldown = skill.getVariableOrNull("cooldown")?.run(options)?.getNow(null)?.cint
        if (cooldown != null) {
            Cooler.INSTANCE.set(player, skill, cooldown)
        }
    }

}
