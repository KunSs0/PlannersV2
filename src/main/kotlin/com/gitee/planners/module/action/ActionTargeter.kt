package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope

/**
 * 目标器。
 *
 * 目标器把当前容器解析为新的容器；结果为空时跳过该动作，不视为运行错误。
 * 严格按书写顺序执行，因此排序必须写在截断之前。
 */
interface ActionTargeter {

    /**
     * 目标器主键，小写。
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
     * 加载期参数校验。
     *
     * @param arguments 目标器参数。
     */
    fun validate(arguments: ActionArguments) {
    }

    /**
     * 解析目标容器。
     *
     * @param scope 当前执行上下文。
     * @param arguments 目标器参数。
     * @param input 上游容器；源目标器忽略该值，过滤器在其基础上筛选。
     * @return 解析后的容器。
     */
    fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget>
}
