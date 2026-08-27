package com.gitee.planners.api

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.directing.DirectingResult
import com.gitee.planners.api.event.player.PlayerSkillCastEvent
import com.gitee.planners.api.event.player.PlayerSkillCastEvent.Check
import com.gitee.planners.api.job.Variable
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.ExecutableResult
import com.gitee.planners.core.skill.directing.DirectingSessionManager
import com.gitee.planners.core.skill.precondition.CastPreCondition
import com.gitee.planners.core.skill.precondition.CastPreConditionFeedback
import com.gitee.planners.core.skill.precondition.CastPreConditionResult
import com.gitee.planners.core.skill.precondition.DefaultCastPreConditionFeedback
import com.gitee.planners.core.skill.precondition.builtin.CooldownPreCondition
import com.gitee.planners.core.skill.precondition.builtin.MagicPointPreCondition
import com.gitee.planners.core.skilltree.SkillTreeNodeEffectService
import com.gitee.planners.module.script.ScriptManager
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
        val level = SkillTreeNodeEffectService.getSkillLevel(player.plannersTemplate, skill.id)
        val options = ScriptOptions.forSkill(player, level, skill)
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
        val level = SkillTreeNodeEffectService.getSkillLevel(player.plannersTemplate, skill.id)
        return ScriptOptions.forSkill(player, level, skill)
    }

    /**
     * 创建技能选项
     */
    fun newOptions(player: Player, skill: PlayerSkill): ScriptOptions {
        val level = SkillTreeNodeEffectService.getSkillLevel(player.plannersTemplate, skill.id)
        return ScriptOptions.forSkill(player, level, skill.immutable)
    }

    /**
     * 创建技能选项
     */
    fun newOptions(player: Player, skill: ImmutableSkill, level: Int): ScriptOptions {
        return ScriptOptions.forSkill(player, level, skill)
    }

    /**
     * 执行玩家技能 action 中定义的回调函数。
     *
     * @param player 玩家
     * @param skill 玩家技能
     * @param method 回调函数名
     * @param variables 额外上下文变量
     * @param payload 回调负载
     * @return 函数存在并完成调用时返回 true，函数不存在时返回 false
     */
    fun executeSkillCallback(player: Player, skill: PlayerSkill, method: String, variables: Map<String, Any>, payload: Map<String, Any>): Boolean {
        val extraVariables = LinkedHashMap<String, Any>()
        extraVariables.putAll(variables)
        val level = SkillTreeNodeEffectService.getSkillLevel(player.plannersTemplate, skill.id)
        val options = ScriptOptions.forSkill(player, level, skill.immutable, extraVariables)
        val result = skill.immutable.invokeActionFunction(method, options, payload)
        return result != null
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
        return cast(player, skill, "")
    }

    /**
     * 以物理按键来源释放玩家技能。
     *
     * @param player 当前施法者。
     * @param skill 当前玩家技能。
     * @param sourceKey 触发本次释放的物理按键标识；非按键调用传入空字符串。
     * @return 当前释放结果。
     */
    fun cast(player: Player, skill: PlayerSkill, sourceKey: String): ExecutableResult {
        if (!Check(player, skill).call()) {
            return ExecutableResult.cancelledWithEvent()
        }
        val options = newOptions(player, skill)
        val sortedConditions = castPreConditions.sortedBy { it.priority }
        for (condition in sortedConditions) {
            val result = condition.verify(player, skill, options)
            if (result is CastPreConditionResult.Failure) {
                castPreConditionFeedback.onFailed(player, result)
                return ExecutableResult.preConditionFailed(result)
            }
        }
        val directing = skill.immutable.directing
        if (directing != null) {
            if (sourceKey.isEmpty()) {
                return ExecutableResult.cancelledWithEvent()
            }
            val started = DirectingSessionManager.start(player, skill, sourceKey) { result ->
                continueAfterDirecting(player, skill, options, sortedConditions, result)
            }
            if (!started) {
                return ExecutableResult.cancelledWithEvent()
            }
            return ExecutableResult.intercepted("directing:${directing.type}")
        }
        if (!PlayerSkillCastEvent.Pre(player, skill).call()) {
            return ExecutableResult.cancelledWithEvent()
        }
        val interceptor = skillInputExecHooks.firstOrNull()
        if (interceptor != null) {
            val context = SkillInputExec.Context(player, skill) { _ ->
                continueCast(player, skill, options, sortedConditions, null)
            }
            interceptor.intercept(context)
            return ExecutableResult.intercepted(interceptor.javaClass.simpleName)
        }
        return continueCast(player, skill, options, sortedConditions, null)
    }

    /**
     * 指向确认后进入常规释放后半段。
     *
     * @param player 当前施法者。
     * @param skill 当前玩家技能。
     * @param options 已在按下时计算的技能选项。
     * @param sortedConditions 已排序的释放前条件。
     * @param directing 已确认的指向性结果。
     * @return 最终释放结果。
     */
    private fun continueAfterDirecting(player: Player, skill: PlayerSkill, options: ScriptOptions, sortedConditions: List<CastPreCondition>, directing: DirectingResult): ExecutableResult {
        if (!PlayerSkillCastEvent.Pre(player, skill).call()) {
            return ExecutableResult.cancelledWithEvent()
        }
        val interceptor = skillInputExecHooks.firstOrNull()
        if (interceptor != null) {
            val context = SkillInputExec.Context(player, skill) { _ ->
                continueCast(player, skill, options, sortedConditions, directing)
            }
            interceptor.intercept(context)
            return ExecutableResult.intercepted(interceptor.javaClass.simpleName)
        }
        return continueCast(player, skill, options, sortedConditions, directing)
    }

    /**
     * 执行最终校验、资源消耗和技能脚本。
     *
     * @param player 当前施法者。
     * @param skill 当前玩家技能。
     * @param options 技能选项。
     * @param sortedConditions 已排序的释放前条件。
     * @param directing 已确认的指向性结果；普通技能为 null。
     * @return 最终释放结果。
     */
    private fun continueCast(player: Player, skill: PlayerSkill, options: ScriptOptions, sortedConditions: List<CastPreCondition>, directing: DirectingResult?): ExecutableResult {
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
        val level = SkillTreeNodeEffectService.getSkillLevel(player.plannersTemplate, skill.id)
        val variables = LinkedHashMap<String, Any?>()
        if (directing != null) {
            variables["directing"] = directing
        }
        skill.immutable.execute(ProxyTarget.BukkitEntity(player), level, variables)
        PlayerSkillCastEvent.Post(player, skill).call()
        return ExecutableResult.successful()
    }

}
