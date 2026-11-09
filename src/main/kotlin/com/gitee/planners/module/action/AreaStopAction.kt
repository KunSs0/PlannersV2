package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.Entity
import taboolib.common.platform.function.warning

/**
 * `area.stop`：结束当前区域主名下指定分类的全部区域实例。
 *
 * 只影响调用者自己的实例，其它玩家的同分类区域不受影响；业务想"替换旧区域"时先调用本动作。
 */
object AreaStopAction : Action {

    override val id = "area.stop"

    override val aliases = listOf("area.stop")

    override val acceptsEmptyTargets = true

    override fun validate(arguments: ActionArguments) {
        arguments.require("id")
        arguments.rejectUnknown("id")
    }

    override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
        val id = arguments.requireText(scope, "id")
        val owner = scope.caster
        if (owner !is Entity) {
            throw IllegalStateException("${arguments.path} 需要玩家作为区域主。")
        }
        val removed = AreaRegistry.stopOwned(owner.uniqueId, id)
        if (removed == 0) {
            warning("${arguments.path} 当前区域主没有 id 为 '" + id + "' 的区域实例。")
        }
    }
}
