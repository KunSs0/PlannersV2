package com.gitee.planners.api

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.event.player.PlayerSkillCastEvent
import com.gitee.planners.api.event.player.PlayerSkillCastEvent.Check
import com.gitee.planners.api.job.Variable
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.ExecutableResult
import com.gitee.planners.core.skill.precondition.CastPreCondition
import com.gitee.planners.core.skill.precondition.CastPreConditionFeedback
import com.gitee.planners.core.skill.precondition.CastPreConditionResult
import com.gitee.planners.core.skill.precondition.DefaultCastPreConditionFeedback
import com.gitee.planners.core.skill.precondition.builtin.CooldownPreCondition
import com.gitee.planners.core.skill.precondition.builtin.MagicPointPreCondition
import com.gitee.planners.module.script.ScriptOptions
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList

object PlannersAPI {

    private val skillInputExecHooks = CopyOnWriteArrayList<SkillInputExecHook>()

    /** 释放前条件列表 */
    private val castPreConditions = CopyOnWriteArrayList<CastPreCondition>()

    /** 释放前条件失败时的反馈实现 */
    private var castPreConditionFeedback: CastPreConditionFeedback = DefaultCastPreConditionFeedback()

    init {
        // 注册内置释放前条件
        castPreConditions.add(CooldownPreCondition())
        castPreConditions.add(MagicPointPreCondition())
    }

    fun registerSkillInputExecHook(hook: SkillInputExecHook) {
        skillInputExecHooks.add(hook)
    }

    /**
     * 注册释放前条件。
     *
     * @param condition 条件实现
     */
    fun registerCastPreCondition(condition: CastPreCondition) {
        castPreConditions.add(condition)
    }

    /**
     * 替换释放前条件失败时的反馈实现。
     *
     * @param feedback 反馈实现
     */
    fun setCastPreConditionFeedback(feedback: CastPreConditionFeedback) {
        castPreConditionFeedback = feedback
    }

    /**
     * 释放技能（无冷却版，直接执行脚本）。
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
     * 流程: Check事件 → 释放前条件校验(按priority排序) → Pre事件 → (interceptor?) → 最终校验 → 消耗资源 → execute → Post
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

        val options = newOptions(player, skill)

        // ② 释放前条件校验（按 priority 升序）
        val sortedConditions = castPreConditions.sortedBy { it.priority }
        for (condition in sortedConditions) {
            val result = condition.verify(player, skill, options)
            if (result is CastPreConditionResult.Failure) {
                castPreConditionFeedback.onFailed(player, result)
                return ExecutableResult.preConditionFailed(result)
            }
        }

        // ③ Pre 事件：最后确认
        if (!PlayerSkillCastEvent.Pre(player, skill).call()) {
            return ExecutableResult.cancelledWithEvent()
        }

        // ④ 全局 SkillInputExecHook
        val interceptor = skillInputExecHooks.firstOrNull()
        if (interceptor != null) {
            val ctx = SkillInputExec.Context(player, skill) {
                continueCast(player, skill, options, sortedConditions)
            }
            interceptor.intercept(ctx)
            return ExecutableResult.intercepted(interceptor.javaClass.simpleName)
        }

        // ⑤ 最终校验 + 消耗资源 + execute + Post
        return continueCast(player, skill, options, sortedConditions)
    }

    private fun continueCast(
        player: Player,
        skill: PlayerSkill,
        options: ScriptOptions,
        sortedConditions: List<CastPreCondition>
    ): ExecutableResult {
        for (condition in sortedConditions) {
            val result = condition.verify(player, skill, options)
            if (result is CastPreConditionResult.Failure) {
                castPreConditionFeedback.onFailed(player, result)
                return ExecutableResult.preConditionFailed(result)
            }
        }

        for (condition in sortedConditions) {
            condition.consume(player, skill, options)
        }

        skill.immutable.execute(ProxyTarget.BukkitEntity(player), skill.level)
        PlayerSkillCastEvent.Post(player, skill).call()
        return ExecutableResult.successful()
    }

}
