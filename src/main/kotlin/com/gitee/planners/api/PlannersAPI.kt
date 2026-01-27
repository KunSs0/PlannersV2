package com.gitee.planners.api

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.event.player.PlayerSkillCastEvent
import com.gitee.planners.api.job.Variable
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.ExecutableResult
import com.gitee.planners.core.skill.cooler.Cooler
import com.gitee.planners.module.fluxon.FluxonScriptOptions
import com.gitee.planners.core.player.magic.MagicPoint.magicPoint
import org.bukkit.entity.Player
import taboolib.common5.cint
import java.util.concurrent.CompletableFuture

object PlannersAPI {

    /**
     * 释放技能
     *
     * @param player 玩家
     * @param skill 技能
     * @return 释放结果
     */
    fun cast(player: Player, skill: ImmutableSkill, level: Int): CompletableFuture<Any?> {
        return skill.execute(TargetBukkitEntity(player), level)
    }

    /**
     * 获取技能变量
     *
     * @param player 玩家
     * @param skill 技能
     * @param variable 变量
     * @return 变量值
     */
    fun getVariableValue(player: Player, skill: ImmutableSkill, variable: Variable): CompletableFuture<Any?> {
        val level = player.plannersTemplate.getRegisteredSkillOrNull(skill.id)?.level ?: 1
        val options = FluxonScriptOptions.forSkill(player, level)
        return variable.run(options)
    }

    /**
     * 获取技能变量
     *
     * @param player 玩家
     * @param skill 技能
     * @param id 变量id
     * @return 变量值
     */
    fun getVariableValue(player: Player, skill: ImmutableSkill, id: String): CompletableFuture<Any?> {
        val variable = skill.getVariableOrNull(id)
            ?: error("Variable $id not found in skill ${skill.id}")
        return getVariableValue(player, skill, variable)
    }

    /**
     * 创建技能选项
     */
    fun newOptions(player: Player, skill: ImmutableSkill): FluxonScriptOptions {
        val level = player.plannersTemplate.getRegisteredSkillOrNull(skill.id)?.level ?: 1
        return FluxonScriptOptions.forSkill(player, level)
    }

    /**
     * 创建技能选项
     */
    fun newOptions(player: Player, skill: PlayerSkill): FluxonScriptOptions {
        return FluxonScriptOptions.forSkill(player, skill.level)
    }

    /**
     * 创建技能选项
     */
    fun newOptions(player: Player, skill: ImmutableSkill, level: Int): FluxonScriptOptions {
        return FluxonScriptOptions.forSkill(player, level)
    }

    /**
     * 释放技能,会记录冷却
     * @param player 玩家
     * @param skill 技能
     * @return 释放结果
     */
    fun cast(player: Player, skill: PlayerSkill): ExecutableResult {
        if (!PlayerSkillCastEvent.Pre(player, skill).call()) {
            return ExecutableResult.cancelledWithEvent()
        }

        // 优先检查冷却
        if (Cooler.INSTANCE.get(player, skill) > 0L) {
            return ExecutableResult.cooling()
        }

        val options = newOptions(player, skill)

        // 检查魔法值
        val magicPoint = skill.getVariableOrNull("mp")?.run(options)?.getNow(null)?.cint
        if (magicPoint != null && magicPoint > player.plannersTemplate.magicPoint) {
            return ExecutableResult.magicPointInsufficient()
        }

        val cooldown = skill.getVariableOrNull("cooldown")?.run(options)?.getNow(null)?.cint
        // 计入冷却器
        if (cooldown != null) {
            Cooler.INSTANCE.set(player, skill, cooldown)
        }
        // 扣除魔法值
        if (magicPoint != null) {
            player.plannersTemplate.magicPoint -= magicPoint
        }
        skill.immutable.execute(TargetBukkitEntity(player), skill.level)
        PlayerSkillCastEvent.Post(player, skill).call()
        return ExecutableResult.successful()
    }

}
