package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.Location
import org.bukkit.entity.Entity

/**
 * `area.contains`：判定当前作用点是否落在指定分类的任一区域实例内。
 *
 * 作用点取当前目标，没有目标时退回施法者；区域只回答几何问题，不涉及任何业务数值。
 */
object AreaContainsCondition : ActionCondition {

    override val id = "area.contains"

    override val aliases = listOf("area.contains")

    override val scope = ActionScope.SKILL

    override fun validate(arguments: ActionArguments) {
        arguments.require("id")
        arguments.rejectUnknown("id")
    }

    override fun test(scope: QueryScope, arguments: ActionArguments, compiled: Any?): Boolean {
        val id = arguments.requireText(scope, "id")
        val position = resolvePosition(scope)
        if (position == null) {
            return false
        }
        return AreaRegistry.contains(position, id)
    }

    /**
     * 取判定位置。
     *
     * @param scope 执行上下文。
     * @return 坐标；目标与施法者都不是实体或坐标时返回 null。
     */
    private fun resolvePosition(scope: QueryScope): Location? {
        val target = scope.target
        if (target is Location) {
            return target
        }
        if (target is Entity) {
            return target.location
        }
        val sender = scope.sender
        if (sender is Location) {
            return sender
        }
        if (sender is Entity) {
            return sender.location
        }
        return null
    }
}
