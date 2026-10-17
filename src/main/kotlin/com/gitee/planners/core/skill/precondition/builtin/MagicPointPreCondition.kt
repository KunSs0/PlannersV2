package com.gitee.planners.core.skill.precondition.builtin

import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.player.magic.MagicPointProvider
import com.gitee.planners.core.skill.precondition.CastPreCondition
import com.gitee.planners.core.skill.precondition.CastPreConditionResult
import com.gitee.planners.core.skill.context.SkillExecutionContext
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

    /**
     * 使用提供器的整数门槛检查本次法力需求，不读取或写回余额快照。
     *
     * @param player 释放技能的玩家。
     * @param skill 本次玩家技能。
     * @param execution 已计算技能变量的执行上下文。
     * @return 余额不足时的失败信息，未配置消耗或余额足够时为 null。
     */
    override fun verify(player: Player, skill: PlayerSkill, execution: SkillExecutionContext): CastPreConditionResult? {
        // 校验只读取展示门槛；真正消费由提供器再次检查并操作权威存储。
        val cost = execution.getVariable("mp")?.cint
        if (cost == null) {
            return null
        }
        val current = MagicPointProvider.INSTANCE.getPoint(player)
        if (cost > current) {
            return CastPreConditionResult.Failure(this, mapOf(
                "required" to cost,
                "current" to current
            ))
        }
        return null
    }

    /**
     * 通过提供器的扣费接口消费法力，保留外部存储中的小数余额。
     *
     * @param player 释放技能的玩家。
     * @param skill 本次玩家技能。
     * @param execution 已计算技能变量的执行上下文。
     */
    override fun consume(player: Player, skill: PlayerSkill, execution: SkillExecutionContext) {
        val cost = execution.getVariable("mp")?.cint
        if (cost != null) {
            // 扣费时再次验证真实余额，禁止使用整数投影读改写。
            check(MagicPointProvider.INSTANCE.consumePoint(player, cost)) { "技能扣费时法力不足: $cost" }
        }
    }

}
