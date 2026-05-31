package com.gitee.planners.api

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.event.player.PlayerSkillCastEvent
import com.gitee.planners.api.event.player.PlayerSkillCastEvent.Check
import com.gitee.planners.api.job.Variable
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.player.magic.MagicPointProvider.Companion.magicPoint
import com.gitee.planners.core.skill.ExecutableResult
import com.gitee.planners.core.skill.cooler.Cooler
import com.gitee.planners.module.script.ScriptOptions
import org.bukkit.entity.Player
import taboolib.common5.cint
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList

object PlannersAPI {

    private val skillInputExecHooks = CopyOnWriteArrayList<SkillInputExecHook>()

    fun registerSkillInputExecHook(hook: SkillInputExecHook) {
        skillInputExecHooks.add(hook)
    }

    /**
     * 释放技能
     *
     * @param player 玩家
     * @param skill 技能
     * @return 释放结果
     */
    fun cast(player: Player, skill: ImmutableSkill, level: Int): CompletableFuture<Any?> {
        return skill.execute(ProxyTarget.BukkitEntity(player), level)
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
        val options = ScriptOptions.forSkill(player, level)
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
    fun newOptions(player: Player, skill: ImmutableSkill): ScriptOptions {
        val level = player.plannersTemplate.getRegisteredSkillOrNull(skill.id)?.level ?: 1
        return ScriptOptions.forSkill(player, level)
    }

    /**
     * 创建技能选项
     */
    fun newOptions(player: Player, skill: PlayerSkill): ScriptOptions {
        return ScriptOptions.forSkill(player, skill.level)
    }

    /**
     * 创建技能选项
     */
    fun newOptions(player: Player, skill: ImmutableSkill, level: Int): ScriptOptions {
        return ScriptOptions.forSkill(player, level)
    }

    /**
     * 释放技能，会记录冷却。
     *
     * 流程: Check → CD检查 → MP检查 → Pre → (interceptor?) → execute → Post
     *
     * @param player 玩家
     * @param skill 技能
     * @return 释放结果
     */
    fun cast(player: Player, skill: PlayerSkill): ExecutableResult {
        // ① Check 事件：外部插件检查条件（如被眩晕）
        if (!Check(player, skill).call()) {
            return ExecutableResult.cancelledWithEvent()
        }

        // ② CD 检查
        if (Cooler.INSTANCE.get(player, skill) > 0L) {
            return ExecutableResult.cooling()
        }

        val options = newOptions(player, skill)

        // ③ MP 检查
        val magicPoint = skill.getVariableOrNull("mp")?.run(options)?.getNow(null)?.cint
        if (magicPoint != null && magicPoint > player.plannersTemplate.magicPoint) {
            return ExecutableResult.magicPointInsufficient()
        }

        // ④ Pre 事件：最后确认
        if (!PlayerSkillCastEvent.Pre(player, skill).call()) {
            return ExecutableResult.cancelledWithEvent()
        }

        // ⑤ 锁定资源
        val cooldown = skill.getVariableOrNull("cooldown")?.run(options)?.getNow(null)?.cint
        if (cooldown != null) {
            Cooler.INSTANCE.set(player, skill, cooldown)
        }
        if (magicPoint != null) {
            player.plannersTemplate.magicPoint -= magicPoint
        }

        // ⑥ 全局 SkillInputExecHook
        val interceptor = skillInputExecHooks.firstOrNull()
        if (interceptor != null) {
            val ctx = SkillInputExec.Context(player, skill) {
                skill.immutable.execute(ProxyTarget.BukkitEntity(player), skill.level)
                PlayerSkillCastEvent.Post(player, skill).call()
                ExecutableResult.successful()
            }
            interceptor.intercept(ctx)
            return ExecutableResult.intercepted(interceptor.javaClass.simpleName)
        }

        // ⑦ execute + Post
        skill.immutable.execute(ProxyTarget.BukkitEntity(player), skill.level)
        PlayerSkillCastEvent.Post(player, skill).call()
        return ExecutableResult.successful()
    }

}
