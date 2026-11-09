package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope

/**
 * `finder.set`：把本行解析出的目标存进命名槽，供后续行用 `<finder.<名字>>` 读取。
 *
 * 槽位只存在本次执行的作用域，跨执行（例如动画前后两个回调）不共享，
 * 需要跨执行时用 `meta.set{value=<finder.<名字>>}` 落到玩家标记上。
 */
object FinderSetAction : Action {

    override val id = "finder.set"

    override val aliases = listOf("finder.set")

    override val acceptsEmptyTargets = true

    override fun validate(arguments: ActionArguments) {
        arguments.require("id")
        arguments.rejectUnknown("id")
    }

    override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
        val key = arguments.requireText(scope, "id")
        if (key.isBlank()) {
            throw IllegalArgumentException("${arguments.path} id 不能为空。")
        }
        // 存快照，后续行重新解析目标器不会影响已存内容。
        scope.finderCache[key] = ArrayList(targets)
    }
}
