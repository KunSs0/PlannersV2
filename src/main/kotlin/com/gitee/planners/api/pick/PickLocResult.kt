package com.gitee.planners.api.pick

import org.bukkit.Location

/**
 * 以世界坐标为结果的指向确认结果。
 *
 * Planners 通过该结果把确认到的坐标绑定成动作上下文的当前目标，使 `@target` 目标器、
 * `<target.*>` 查询根与 `area.start` 的锚点解析读到同一个坐标。
 */
class PickLocResult(val location: Location) : PickResult {

    override val type = PositionPickDefinition.TYPE
}
