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

    fun consume(
        player: Player,
        skill: PlayerSkill,
        execution: SkillExecutionContext,
        configs: List<SkillCastConditionConfig>,
        timing: CastCostTiming
    ) {
        val reservations = ArrayList<CastCostReservation>()
        try {
            for (config in configs) {
                if (config.costTiming != timing) {
                    continue
                }
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
