package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.Entity

/**
 * `@finder.capture`：读取会话捕捉累积到的目标。
 *
 * 目标按首次捕捉顺序返回，已失效的目标会被跳过；会话不存在（未启动或已结束）时报错，
 * 避免把“漏写 start”误当成“这轮没有目标”。
 */
object CaptureTargeter : ActionTargeter {

    override val id = "finder.capture"

    override val aliases = listOf("finder.capture")

    override val scope = ActionScope.SKILL

    override fun validate(arguments: ActionArguments) {
        arguments.require("id")
        arguments.rejectUnknown("id")
    }

    override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
        val id = arguments.requireText(scope, "id")
        val owner = scope.caster
        if (owner !is Entity) {
            return emptyList()
        }
        val session = CaptureRegistry.find(owner.uniqueId, id)
        if (session == null) {
            throw IllegalArgumentException("${arguments.path} 引用了不存在或已结束的捕捉 '$id'。")
        }
        return session.targets()
    }
}
