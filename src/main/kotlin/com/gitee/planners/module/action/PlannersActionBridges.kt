package com.gitee.planners.module.action

import com.gitee.action.api.Action as ActionFacade
import com.gitee.action.api.model.ActionArguments as ApiArguments
import com.gitee.action.api.model.ActionContext as ApiContext
import com.gitee.action.api.model.ActionOutcome as ApiOutcome
import com.gitee.action.api.model.ActionTarget as ApiTarget
import com.gitee.action.api.spi.ActionCondition as ApiCondition
import com.gitee.action.api.spi.ActionMechanism as ApiMechanism
import com.gitee.action.api.spi.ActionTargeter as ApiTargeter
import com.gitee.action.api.spi.ActionTrigger as ApiTrigger
import com.gitee.action.api.statement.TriggerFacts
import com.gitee.action.api.statement.TriggerKind

/**
 * 技能动作到 Action 的适配层。
 *
 * 技能侧的实现保持原样，由适配器把 Action 的上下文、参数与目标转换为技能语义后委派执行。
 * 适配器在注册期生成，因此解析期就能通过 Action 的注册表找到这些条目。
 */
object PlannersActionBridges {

    /**
     * 把技能注册表中已有的条目整体登记到 Action。
     *
     * <p>只在插件启用阶段调用一次：此时技能侧的内置条目与外部插件条目都已注册，
     * 一次性适配后即可由 Action 的解析器与引擎直接使用。</p>
     *
     * @param owner 注册方，用于重载时撤销
     */
    fun installRegistry(owner: Any) {
        for (id in ActionRegistry.actionIds()) {
            val action = ActionRegistry.action(id)
            if (action != null) {
                registerMechanism(action, owner)
            }
        }
        for (id in ActionRegistry.conditionIds()) {
            val condition = ActionRegistry.condition(id)
            if (condition != null) {
                registerCondition(condition, owner)
            }
        }
        for (id in ActionRegistry.targeterIds()) {
            val targeter = ActionRegistry.targeter(id)
            if (targeter != null) {
                registerTargeter(targeter, owner)
            }
        }
        for (id in ActionRegistry.triggerIds()) {
            val trigger = ActionRegistry.trigger(id)
            if (trigger != null) {
                registerTrigger(trigger, owner)
            }
        }
    }

    /**
     * 注册技能动作机制。
     *
     * @param delegate 技能动作
     * @param owner 注册方，用于重载时撤销
     */
    fun registerMechanism(delegate: Action, owner: Any) {
        ActionFacade.mechanisms().register(MechanismBridge(delegate), owner)
    }

    /**
     * 注册技能条件。
     *
     * @param delegate 技能条件
     * @param owner 注册方
     */
    fun registerCondition(delegate: ActionCondition, owner: Any) {
        ActionFacade.conditions().register(ConditionBridge(delegate), owner)
    }

    /**
     * 注册技能目标器。
     *
     * @param delegate 技能目标器
     * @param owner 注册方
     */
    fun registerTargeter(delegate: ActionTargeter, owner: Any) {
        ActionFacade.targeters().register(TargeterBridge(delegate), owner)
    }

    /**
     * 注册技能触发器。
     *
     * @param delegate 技能触发器
     * @param owner 注册方
     */
    fun registerTrigger(delegate: ActionTrigger, owner: Any) {
        ActionFacade.triggers().register(TriggerBridge(delegate), owner)
    }

    /**
     * 把 Action 参数转换为技能参数。
     *
     * @param arguments Action 参数
     * @return 技能参数
     */
    private fun convert(arguments: ApiArguments): ActionArguments {
        return ActionArguments(arguments.all(), arguments.getPath())
    }

    /**
     * 动作机制适配器。
     *
     * @property delegate 技能动作
     */
    private class MechanismBridge(private val delegate: Action) : ApiMechanism {

        override fun getId(): String {
            return delegate.id
        }

        override fun getAliases(): List<String> {
            return delegate.aliases
        }

        override fun validate(arguments: ApiArguments) {
            delegate.validate(convert(arguments))
        }

        override fun execute(
            context: ApiContext,
            arguments: ApiArguments,
            targets: List<ApiTarget>,
        ): ApiOutcome {
            delegate.execute(
                PlannersActionContexts.scopeOf(context),
                convert(arguments),
                PlannersActionTargets.toPlannersAll(targets),
            )
            return ApiOutcome.none()
        }
    }

    /**
     * 条件适配器。
     *
     * @property delegate 技能条件
     */
    private class ConditionBridge(private val delegate: ActionCondition) : ApiCondition {

        override fun getId(): String {
            return delegate.id
        }

        override fun getAliases(): List<String> {
            return delegate.aliases
        }

        override fun validate(arguments: ApiArguments) {
            delegate.validate(convert(arguments))
        }

        override fun compile(arguments: ApiArguments): Any? {
            return delegate.compile(convert(arguments))
        }

        override fun test(context: ApiContext, arguments: ApiArguments, compiled: Any?): Boolean {
            return delegate.test(PlannersActionContexts.scopeOf(context), convert(arguments), compiled)
        }
    }

    /**
     * 目标器适配器。
     *
     * @property delegate 技能目标器
     */
    private class TargeterBridge(private val delegate: ActionTargeter) : ApiTargeter {

        override fun getId(): String {
            return delegate.id
        }

        override fun getAliases(): List<String> {
            return delegate.aliases
        }

        override fun isSource(): Boolean {
            return PlannersActionSources.isSource(delegate.id)
        }

        override fun validate(arguments: ApiArguments) {
            delegate.validate(convert(arguments))
        }

        override fun resolve(
            context: ApiContext,
            arguments: ApiArguments,
            input: List<ApiTarget>,
        ): List<ApiTarget> {
            val resolved = delegate.resolve(
                PlannersActionContexts.scopeOf(context),
                convert(arguments),
                PlannersActionTargets.toPlannersAll(input),
            )
            return PlannersActionTargets.toActionAll(resolved)
        }
    }

    /**
     * 触发器适配器。
     *
     * @property delegate 技能触发器
     */
    private class TriggerBridge(private val delegate: ActionTrigger) : ApiTrigger {

        override fun getId(): String {
            return delegate.id
        }

        override fun getAliases(): List<String> {
            return delegate.aliases
        }

        override fun getKind(): TriggerKind {
            if (PlannersActionSources.isTimer(delegate.id)) {
                return TriggerKind.TIMER
            }
            return TriggerKind.EVENT
        }

        override fun validate(arguments: ApiArguments) {
            delegate.validate(convert(arguments))
        }

        override fun matches(context: ApiContext, arguments: ApiArguments, facts: TriggerFacts): Boolean {
            return delegate.matches(PlannersActionContexts.scopeOf(context), convert(arguments))
        }

        override fun getProvidedTargets(): Set<String> {
            return delegate.providedTargets
        }

        override fun getInterval(arguments: ApiArguments): Int? {
            return delegate.interval(convert(arguments))
        }

        override fun getIdentifier(arguments: ApiArguments): String? {
            return delegate.identifier(convert(arguments))
        }

        override fun getDelay(arguments: ApiArguments): Int {
            return delegate.delay(convert(arguments))
        }

        override fun getTimes(arguments: ApiArguments): Int {
            return delegate.times(convert(arguments))
        }
    }
}
