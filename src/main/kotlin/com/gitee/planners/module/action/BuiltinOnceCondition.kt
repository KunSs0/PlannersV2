package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope

/**
 * `once`：本次执行内同一键只成立一次。
 *
 * 用于「一次施法只发生一次」的效果，例如首个命中才加成的奖励、最多三次的命中音效。
 * 状态挂在执行上下文上，因此在 `on-hit` 这类逐目标流程里天然只对第一个目标成立，
 * 下一次施放（新的一次执行）重新计数。
 *
 * 键名由调用方自己给，相同键共享同一个计数位置。
 */
object BuiltinOnceCondition : ActionCondition {

    override val id = "once"

    override val aliases = listOf("once")

    override fun validate(arguments: ActionArguments) {
        arguments.require("key")
        arguments.rejectUnknown("key")
    }

    override fun test(scope: QueryScope, arguments: ActionArguments, compiled: Any?): Boolean {
        val key = arguments.requireText(scope, "key")
        return scope.onceKeys.add(key)
    }
}
