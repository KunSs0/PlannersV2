package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope

/**
 * 延迟绑定的动作引用。
 *
 * 解析配置时外部插件可能还没注册自己的动作：Planners 在自身 `onEnable` 里解析技能，
 * 而 `fc.*` 这类动作要等对应插件启用后才注册。因此带命名空间的未知动作先留成引用，
 * 等执行或启动后校验时再解析。
 *
 * 内置动作拼错仍然立即报错，只有含 `.` 的外部命名空间才延迟。
 *
 * @property actionId 动作主键。
 * @property path 配置路径。
 * @property arguments 解析时得到的参数，供校验阶段复用。
 */
class DeferredAction(
    override val id: String,
    val path: String,
    val arguments: ActionArguments,
) : Action {

    /** 延迟引用只按主键解析，不引入别名。 */
    override val aliases: List<String>
        get() {
            return emptyList()
        }

    /** 解析阶段作用域未知，延迟到执行时由真实动作决定。 */
    override val scope = ActionScope.SKILL

    /** 执行阶段会真正执行动作，因此允许空目标。 */
    override val acceptsEmptyTargets = true

    /**
     * 校验阶段跳过：此时动作实现可能尚未注册。
     *
     * 参数校验推迟到 [verify] 里重新执行。
     *
     * @param arguments 动作参数。
     */
    override fun validate(arguments: ActionArguments) {
        return
    }

    /**
     * 解析成真实动作。
     *
     * @return 已注册的动作实现。
     * @throws IllegalStateException 动作始终没有注册时抛出。
     */
    fun resolve(): Action {
        val action = ActionRegistry.action(id)
        if (action == null) {
            throw IllegalStateException(
                "$path references unknown action '$id'; the providing plugin did not register it " +
                    "or was disabled. Registered actions: " + ActionRegistry.actionIds().toString(),
            )
        }
        return action
    }

    /**
     * 启动后校验：能解析时按真实动作重跑参数校验。
     *
     * @throws IllegalArgumentException 参数不符合目标动作要求时抛出。
     */
    fun verify() {
        val action = resolve()
        action.validate(arguments)
    }

    /**
     * 执行：转交已注册的动作实现。
     *
     * @param scope 执行上下文。
     * @param arguments 动作参数。
     * @param targets 目标列表。
     */
    override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
        resolve().execute(scope, arguments, targets)
    }
}
