package com.gitee.planners.api

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.script.KetherScript.Companion.PARSER_INT
import com.gitee.planners.api.common.script.KetherScript.Companion.getNow
import com.gitee.planners.api.common.script.KetherScriptOptions
import com.gitee.planners.api.event.player.PlayerRouteEvent
import com.gitee.planners.api.event.player.PlayerSkillCastEvent
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.ExecutableResult
import com.gitee.planners.core.skill.cooler.Cooler
import com.gitee.planners.module.kether.context.ImmutableSkillContext
import com.gitee.planners.module.magic.MagicPoint.magicPoint
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
    fun cast(player: Player, skill: ImmutableSkill, level: Int): CompletableFuture<Any> {
        return newCtx(player, skill, level).call()
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
        val ctx = newCtx(player, skill)
        // 检查魔法值
        val magicPoint = skill.getVariableOrNull("mp")?.getNow(ctx.optionsBuilder(), PARSER_INT)
        if (magicPoint != null && magicPoint > player.plannersTemplate.magicPoint) {
            return ExecutableResult.magicPointInsufficient()
        }

        val cooldown = skill.getVariableOrNull("cooldown")?.getNow(ctx.optionsBuilder(), PARSER_INT)
        // 计入冷却器
        if (cooldown != null) {
            Cooler.INSTANCE.set(player, skill, cooldown)
        }
        // 扣除魔法值
        if (magicPoint != null) {
            player.plannersTemplate.magicPoint -= magicPoint
        }
        ctx.call()
        PlayerSkillCastEvent.Post(player, skill).call()
        return ExecutableResult.successful()
    }

    /**
     * 创建技能上下文
     *
     * @param player 玩家
     * @param skill 技能
     *
     * @return ImmutableSkillContext
     */
    fun newCtx(player: Player, skill: PlayerSkill): ImmutableSkillContext {
        return newCtx(player, skill.immutable, skill.level)
    }

    /**
     * 创建技能上下文
     *
     * @param player 玩家
     * @param skill 技能
     * @param level 技能等级
     *
     * @return ImmutableSkillContext
     */
    fun newCtx(player: Player, skill: ImmutableSkill, level: Int): ImmutableSkillContext {
        return ImmutableSkillContext(adaptTarget(player), skill, level)
    }

}
