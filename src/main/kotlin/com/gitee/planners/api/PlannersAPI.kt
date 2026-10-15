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
import com.gitee.planners.core.skill.context.SkillExecutionContext
import com.gitee.planners.core.skill.condition.CastCostTiming
import com.gitee.planners.core.skill.condition.SkillCastCondition
import com.gitee.planners.core.skill.condition.SkillCastConditionRegistry
import com.gitee.planners.core.skill.timing.SkillCastingSessions
import com.gitee.planners.core.skill.timing.SkillTimingService
import com.gitee.planners.core.skill.directing.DirectingSessionManager
import com.gitee.planners.core.skill.precondition.CastPreCondition
import com.gitee.planners.core.skill.precondition.CastPreConditionFeedback
import com.gitee.planners.core.skill.precondition.CastPreConditionResult
import com.gitee.planners.core.skill.precondition.DefaultCastPreConditionFeedback
import com.gitee.planners.core.skill.precondition.builtin.CooldownPreCondition
import com.gitee.planners.core.skill.precondition.builtin.MagicPointPreCondition
import com.gitee.planners.core.skilltree.SkillTreeNodeEffectService
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

    fun registerCastCondition(id: String, condition: SkillCastCondition) {
        SkillCastConditionRegistry.register(id, condition)
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
        val execution = newExecution(player, skill, level)
        return CompletableFuture.completedFuture(execution.getVariable(variable.id))
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
    fun newExecution(player: Player, skill: ImmutableSkill): SkillExecutionContext {
        val level = SkillTreeNodeEffectService.getSkillLevel(player.plannersTemplate, skill.id)
        return newExecution(player, skill, level)
    }

    /**
     * 创建技能选项
     */
    fun newExecution(player: Player, skill: PlayerSkill): SkillExecutionContext {
        val level = SkillTreeNodeEffectService.getSkillLevel(player.plannersTemplate, skill.id)
        return newExecution(player, skill.immutable, level)
    }

    /**
     * 创建技能选项
     */
    fun newExecution(player: Player, skill: ImmutableSkill, level: Int): SkillExecutionContext {
        val execution = SkillExecutionContext.create(ProxyTarget.BukkitEntity(player), level, skill)
        val values = skill.evaluateDisplayVariables(execution)
        for ((id, value) in values) {
            execution.setVariable(id, value)
        }
        return execution
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
        val execution = newExecution(player, skill.immutable, level)
        for ((id, value) in extraVariables) {
            execution.setVariable(id, value)
        }
        val result = skill.immutable.invokeActionFunction(method, execution, payload)
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
     * 在主线程捕获唯一时间快照；施法等待与动画Hook结束后再次校验资源并统一提交。
     *
     * @param player 当前施法者。
     * @param skill 当前玩家技能。
     * @param sourceKey 触发本次释放的物理按键标识；非按键调用传入空字符串。
     * @return 当前释放结果。
     * @throws IllegalArgumentException 时间配置或基础变量非法时抛出。
     * @throws IllegalStateException 非主线程或外部时间Provider缺失时抛出。
     */
    fun cast(player: Player, skill: PlayerSkill, sourceKey: String): ExecutableResult {
        // 等待施法完成期间禁止再次发起技能，避免重复安排资源提交。
        if (SkillCastingSessions.isCasting(player)) {
            return ExecutableResult.cancelledWithEvent()
        }
        if (!Check(player, skill).call()) {
            return ExecutableResult.cancelledWithEvent()
        }
        val execution = newExecution(player, skill)
        // 在条件读取最终时间前准备快照，后续延迟和最终校验不会重复应用急速。
        SkillTimingService.prepare(player, execution)
        val castConditionFailure = SkillCastConditionRegistry.verify(player, skill, execution, skill.immutable.castConditions)
        if (castConditionFailure != null) {
            player.sendMessage(castConditionFailure)
            return ExecutableResult.cancelledWithEvent()
        }
        val sortedConditions = castPreConditions.sortedBy { it.priority }
        for (condition in sortedConditions) {
            val result = condition.verify(player, skill, execution)
            if (result is CastPreConditionResult.Failure) {
                castPreConditionFeedback.onFailed(player, result)
                return ExecutableResult.preConditionFailed(result)
            }
        }
        SkillCastConditionRegistry.consume(player, skill, execution, skill.immutable.castConditions, CastCostTiming.START)
        val directing = skill.immutable.directing
        if (directing != null) {
            if (sourceKey.isEmpty()) {
                return ExecutableResult.cancelledWithEvent()
            }
            val started = DirectingSessionManager.start(player, skill, sourceKey) { result ->
                continueAfterDirecting(player, skill, execution, sortedConditions, result)
            }
            if (!started) {
                return ExecutableResult.cancelledWithEvent()
            }
            return ExecutableResult.intercepted("directing:${directing.type}")
        }
        if (!PlayerSkillCastEvent.Pre(player, skill).call()) {
            return ExecutableResult.cancelledWithEvent()
        }
        return beginCasting(player, skill, execution, sortedConditions, null)
    }

    /**
     * 指向确认后进入常规释放后半段。
     *
     * @param player 当前施法者。
     * @param skill 当前玩家技能。
     * @param execution 已在按下时计算变量和时间快照的执行上下文。
     * @param sortedConditions 已排序的释放前条件。
     * @param directing 已确认的指向性结果。
     * @return 最终释放结果。
     */
    private fun continueAfterDirecting(player: Player, skill: PlayerSkill, execution: SkillExecutionContext, sortedConditions: List<CastPreCondition>, directing: DirectingResult): ExecutableResult {
        if (!PlayerSkillCastEvent.Pre(player, skill).call()) {
            return ExecutableResult.cancelledWithEvent()
        }
        return beginCasting(player, skill, execution, sortedConditions, directing)
    }

    /**
     * 在指向确认与Pre事件通过后，先完成独立施法等待，再进入既有动画Hook。
     *
     * @param player 当前施放者。
     * @param skill 当前玩家技能。
     * @param execution 已捕获最终时间的执行上下文。
     * @param sortedConditions 已排序的最终资源条件。
     * @param directing 本次指向结果，非指向技能为null。
     * @return 瞬发、等待或Hook接管的释放结果。
     */
    private fun beginCasting(player: Player, skill: PlayerSkill, execution: SkillExecutionContext, sortedConditions: List<CastPreCondition>, directing: DirectingResult?): ExecutableResult {
        // 不缩短招式动画；施法等待与动画衔接是两个独立阶段。
        val timing = execution.timing
        check(timing != null) { "技能时间尚未准备：${skill.id}" }
        return SkillCastingSessions.begin(player, timing.castTimeTicks) {
            executeInputHook(player, skill, execution, sortedConditions, directing)
        }
    }

    /**
     * 施法等待完成后进入原有输入Hook与最终提交链。
     *
     * @param player 当前施放者。
     * @param skill 当前玩家技能。
     * @param execution 已准备时间的执行上下文。
     * @param sortedConditions 已排序的资源条件。
     * @param directing 本次指向结果。
     * @return Hook拦截结果或最终释放结果。
     */
    private fun executeInputHook(player: Player, skill: PlayerSkill, execution: SkillExecutionContext, sortedConditions: List<CastPreCondition>, directing: DirectingResult?): ExecutableResult {
        // 每个释放入口均复用原执行上下文，Hook恢复不重新计算变量和急速。
        val interceptor = skillInputExecHooks.firstOrNull()
        if (interceptor != null) {
            val context = SkillInputExec.Context(player, skill) { _ ->
                continueCast(player, skill, execution, sortedConditions, directing)
            }
            interceptor.intercept(context)
            return ExecutableResult.intercepted(interceptor.javaClass.simpleName)
        }
        return continueCast(player, skill, execution, sortedConditions, directing)
    }

    /**
     * 执行最终校验、资源消耗和技能脚本。
     *
     * @param player 当前施法者。
     * @param skill 当前玩家技能。
     * @param execution 已准备变量和唯一时间快照的执行上下文。
     * @param sortedConditions 已排序的释放前条件。
     * @param directing 已确认的指向性结果；普通技能为 null。
     * @return 最终释放结果。
     */
    private fun continueCast(player: Player, skill: PlayerSkill, execution: SkillExecutionContext, sortedConditions: List<CastPreCondition>, directing: DirectingResult?): ExecutableResult {
        val castConditionFailure = SkillCastConditionRegistry.verify(player, skill, execution, skill.immutable.castConditions)
        if (castConditionFailure != null) {
            player.sendMessage(castConditionFailure)
            return ExecutableResult.cancelledWithEvent()
        }
        for (condition in sortedConditions) {
            val result = condition.verify(player, skill, execution)
            if (result is CastPreConditionResult.Failure) {
                castPreConditionFeedback.onFailed(player, result)
                return ExecutableResult.preConditionFailed(result)
            }
        }
        for (condition in sortedConditions) {
            condition.consume(player, skill, execution)
        }
        SkillCastConditionRegistry.consume(player, skill, execution, skill.immutable.castConditions, CastCostTiming.COMMIT)
        if (directing != null) {
            execution.context.directing = directing
            execution.setVariable("directing", directing)
        }
        skill.immutable.execute(execution)
        PlayerSkillCastEvent.Post(player, skill).call()
        return ExecutableResult.successful()
    }

}
