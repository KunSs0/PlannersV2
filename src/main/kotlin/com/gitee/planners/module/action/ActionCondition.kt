package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope

/**
 * 单个条件的扩展接口。
 *
 * 条件在加载期完成参数校验；需要缓存解析结果时通过 [compile] 返回，
 * 运行期每次求值都会原样传回 [test]。
 */
interface ActionCondition {

    /**
     * 条件主键，小写。
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
     * @param arguments 条件参数。
     */
    fun validate(arguments: ActionArguments) {
    }

    /**
     * 加载期编译条件。
     *
     * @param arguments 条件参数。
     * @return 编译结果；不需要缓存时返回 null。
     */
    fun compile(arguments: ActionArguments): Any? {
        return null
    }

    /**
     * 求值条件。
     *
     * @param scope 当前执行上下文。
     * @param arguments 条件参数。
     * @param compiled [compile] 的返回值。
     * @return 条件是否成立。
     */
    fun test(scope: QueryScope, arguments: ActionArguments, compiled: Any?): Boolean
}
