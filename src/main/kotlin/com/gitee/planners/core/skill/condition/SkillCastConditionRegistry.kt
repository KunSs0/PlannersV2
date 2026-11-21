package com.gitee.planners.core.skill.condition

import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.context.SkillExecutionContext
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

object SkillCastConditionRegistry {
    private val conditions = ConcurrentHashMap<String, SkillCastCondition>()

    fun register(id: String, condition: SkillCastCondition) {
        val normalized = id.trim()
        if (normalized.isEmpty()) {
            throw IllegalArgumentException("Skill cast condition id must not be blank")
        }
        if (normalized.contains('.')) {
            throw IllegalArgumentException("Skill cast condition id must not contain '.'")
        }
        conditions[normalized] = condition
    }

    fun getOrNull(id: String): SkillCastCondition? {
        return conditions[id]
    }

    fun verify(
        player: Player,
        skill: PlayerSkill,
        execution: SkillExecutionContext,
        configs: List<SkillCastConditionConfig>
    ): String? {
        for (config in configs) {
            val condition = conditions[config.id]
            if (condition == null) {
                return "未注册技能释放条件：${config.id}"
            }
            val failure = condition.verify(player, skill, execution, config.props)
            if (failure != null) {
                return failure
            }
        }
        return null
    }

    /**
     * 提交全部条件成本。
     *
     * 只在所有条件校验通过后调用一次：逐条准备预留，全部成功后统一提交；
     * 任一环节失败按已准备的预留整体回滚。
     *
     * @param player 施放者。
     * @param skill 技能。
     * @param execution 执行上下文。
     * @param configs 已编译的条件列表。
     */
    fun consume(
        player: Player,
        skill: PlayerSkill,
        execution: SkillExecutionContext,
        configs: List<SkillCastConditionConfig>
    ) {
        val reservations = ArrayList<CastCostReservation>()
        try {
            for (config in configs) {
                val condition = conditions[config.id]
                if (condition == null) {
                    throw IllegalStateException("未注册技能释放条件：${config.id}")
                }
                val reservation = condition.prepareConsume(player, skill, execution, config.props)
                if (reservation != null) {
                    reservations.add(reservation)
                }
            }
        } catch (exception: Throwable) {
            for (reservation in reservations) {
                reservation.rollback()
            }
            throw exception
        }
        try {
            for (reservation in reservations) {
                reservation.commit()
            }
        } catch (exception: Throwable) {
            for (reservation in reservations) {
                reservation.rollback()
            }
            throw exception
        }
    }
}
