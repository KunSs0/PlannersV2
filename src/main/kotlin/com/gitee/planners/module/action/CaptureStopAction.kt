package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.Entity

/**
 * `finder.capture.stop`：提前结束捕捉会话。
 *
 * 只取消会话，不触发结束回调；需要结算时由业务在结束前自行读取 `@finder.capture{id=...}`。
 */
object CaptureStopAction : Action {

    override val id = "finder.capture.stop"

    override val aliases = listOf("finder.capture.stop")

    override val acceptsEmptyTargets = true

    override fun validate(arguments: ActionArguments) {
        arguments.require("id")
        arguments.rejectUnknown("id")
    }

    override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
        val id = arguments.requireText(scope, "id")
        val owner = scope.caster
        if (owner !is Entity) {
            throw IllegalStateException("${arguments.path} 需要实体作为捕捉者。")
        }
        CaptureRegistry.stopOwned(owner.uniqueId, id)
    }
}
