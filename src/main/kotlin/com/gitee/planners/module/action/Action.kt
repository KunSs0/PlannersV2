package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope

/**
 * 单个动作的扩展接口。
 *
 * 动作只负责一次副作用或一次发送，不负责流程推进；条件、目标解析、
 * 触发器与延迟都由运行时统一处理。
 */
interface Action {

    /**
     * 动作主键，小写。跨插件来源使用 `fc.` / `mm.` 前缀。
     */
    val id: String

    /**
     * 允许的别名，包含主键。
     */
    val aliases: List<String>

    /**
     * 允许出现的上下文，缺省为通用。
     */
    val scope: ActionScope
        get() {
            return ActionScope.GENERAL
        }

    /**
     * 目标容器为空时是否仍执行。
     *
     * 默认为 false：目标为空即跳过该行，不视为运行错误。
     * 只有「把当前容器写入缓存」这类动作需要接受空容器。
     */
    val acceptsEmptyTargets: Boolean
        get() {
            return false
        }

    /**
     * 加载期参数校验。
     *
     * @param arguments 动作参数。
     */
    fun validate(arguments: ActionArguments) {
    }

    /**
     * 执行一次动作。
     *
     * @param scope 当前执行上下文。
     * @param arguments 动作参数。
     * @param targets 目标器解析结果；无目标器时为动作自身的默认目标。
     */
    fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>)
}
