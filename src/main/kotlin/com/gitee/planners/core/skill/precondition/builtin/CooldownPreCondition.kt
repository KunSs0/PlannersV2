package com.gitee.planners.core.skill.precondition.builtin

import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.cooler.Cooler
import com.gitee.planners.core.skill.precondition.CastPreCondition
import com.gitee.planners.core.skill.precondition.CastPreConditionResult
import com.gitee.planners.core.skill.context.SkillExecutionContext
import org.bukkit.entity.Player
import taboolib.platform.util.asLangText

/**
 * 冷却检查。
 *
 * 校验技能是否处于冷却中，通过后在 consume 阶段设置冷却。
 * 最终冷却来自本次释放唯一的SkillTimingSnapshot，时间服务负责应用属性收益和取整。
 */
class CooldownPreCondition : CastPreCondition {

    override val key = "cooldown"

    override val priority = 100

    override fun hint(player: Player, failure: CastPreConditionResult.Failure): String {
        return player.asLangText("precondition-cooldown")
    }

    override fun verify(player: Player, skill: PlayerSkill, execution: SkillExecutionContext): CastPreConditionResult? {
        val remaining = Cooler.INSTANCE.get(player, skill)
        if (remaining > 0L) {
            return CastPreConditionResult.Failure(this, mapOf("remaining" to remaining))
        }
        return null
    }

    /**
     * 在最终校验通过后提交本次时间快照中的冷却。
     *
     * @param player 本次施放者。
     * @param skill 当前技能。
     * @param execution 已准备时间快照的执行上下文。
     * @throws IllegalStateException 尚未准备时间快照时抛出。
     */
    override fun consume(player: Player, skill: PlayerSkill, execution: SkillExecutionContext) {
        // 时间由释放开始时的唯一快照提供，最终校验与提交不再次缩短。
        val timing = execution.timing
        check(timing != null) { "技能冷却提交前必须准备时间快照：${skill.id}" }
        val cooldown = timing.cooldownTicks
        if (cooldown != null) {
            Cooler.INSTANCE.set(player, skill, cooldown)
        }
    }

}
