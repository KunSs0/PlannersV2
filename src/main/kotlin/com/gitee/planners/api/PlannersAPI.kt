package com.gitee.planners.api

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.script.KetherScriptOptions
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.ExecutableResult
import com.gitee.planners.core.skill.cooler.Cooler
import com.gitee.planners.module.kether.context.ImmutableSkillContext
import com.gitee.planners.module.magic.MagicPoint.magicPoint
import org.bukkit.entity.Player
import taboolib.common5.cint
import java.util.concurrent.CompletableFuture

object PlannersAPI {

    val PARSE_INT: (Any?) -> Int = { it.cint }

    fun cast(player: Player, skill: ImmutableSkill, level: Int): CompletableFuture<Any> {
        return createImmutableContext(player, skill, level).call()
    }

    /**
     * 释放技能,会记录冷却
     * @param player 玩家
     * @param skill 技能
     * @return 释放结果
     */
    fun cast(player: Player, skill: PlayerSkill): ExecutableResult {
        // 优先检查冷却
        if (Cooler.INSTANCE.get(player, skill) > 0L) {
            return ExecutableResult.cooling()
        }
        // 检查魔法值
        val magicPoint = skill.getVariableOrNull("mp")?.get(createSimpleOptions(player, skill), PARSE_INT)
        if (magicPoint != null && magicPoint > player.plannersTemplate.magicPoint) {
            return ExecutableResult.magicPointInsufficient()
        }

        val cooldown = skill.getVariableOrNull("cooldown")?.get(createSimpleOptions(player, skill), PARSE_INT)
        // 计入冷却器
        if (cooldown != null) {
            Cooler.INSTANCE.set(player, skill, cooldown)
        }
        // 扣除魔法值
        if (magicPoint != null) {
            player.plannersTemplate.magicPoint -= magicPoint
        }
        createImmutableContext(player, skill).call()
        return ExecutableResult.successful()
    }

    private fun createSimpleOptions(player: Player, skill: PlayerSkill): KetherScriptOptions {
        return createImmutableContext(player, skill).optionsBuilder()
    }

    private fun createImmutableContext(player: Player, skill: ImmutableSkill, level: Int): ImmutableSkillContext {
        return ImmutableSkillContext(adaptTarget(player), skill, level)
    }

    private fun createImmutableContext(player: Player, skill: PlayerSkill): ImmutableSkillContext {
        return createImmutableContext(player, skill.immutable, skill.level)
    }

}
