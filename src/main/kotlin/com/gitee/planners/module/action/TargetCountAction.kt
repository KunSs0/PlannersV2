package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope

/**
 * `target.count`：把本行解析出的目标数量写进变量。
 *
 * 用于"按人数决定数值"的场景，例如护盾 `min(每名友方 × min(其他友方数, 4) + 协同, 上限)`。
 * 目标为空时写入 0，不会中断流程。
 */
object TargetCountAction : Action {

    override val id = "target.count"

    override val aliases = listOf("target.count")

    override val acceptsEmptyTargets = true

    override fun validate(arguments: ActionArguments) {
        arguments.require("to")
        arguments.rejectUnknown("to")
    }

    override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
        val key = arguments.requireText(scope, "to")
        if (key.isBlank()) {
            throw IllegalArgumentException("${arguments.path} to 不能为空。")
        }
        val variables = scope.variables
        if (variables !is MutableMap<*, *>) {
            throw IllegalStateException("${arguments.path} 需要一个可写的作用域变量表。")
        }
        val writable = variables as MutableMap<String, Any?>
        writable[key] = targets.size
        // 同一执行内后续行通过 <skill.var.{key}> 读取。
    }
}
