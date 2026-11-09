package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import taboolib.common.platform.function.warning

/**
 * `timer.stop`：取消当前执行上下文里同一 `id` 的全部周期任务。
 *
 * 只能取消本次执行自己启动的任务；其它玩家或其它次执行启动的同名任务不受影响。
 */
object TimerStopAction : Action {

    override val id = "timer.stop"

    override val aliases = listOf("timer.stop")

    override val acceptsEmptyTargets = true

    override fun validate(arguments: ActionArguments) {
        arguments.require("id")
        arguments.rejectUnknown("id")
    }

    override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
        val id = arguments.requireText(scope, "id")
        val cancelled = scope.timerCache.cancel(id)
        if (cancelled == 0) {
            warning("${arguments.path} 当前上下文没有 id 为 '$id' 的定时任务。")
        }
    }
}
