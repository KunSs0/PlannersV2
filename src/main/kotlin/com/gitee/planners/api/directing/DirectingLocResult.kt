package com.gitee.planners.api.directing

import org.bukkit.Location

/**
 * 以世界坐标为结果的指向确认结果。
 *
 * Planners 通过该子类型把确认结果绑定成动作上下文的当前目标，使 `@target` 目标器、
 * `<target.*>` 查询根与 `area.start` 的锚点解析读到同一个坐标；provider 自己的结果
 * 类型仍由 provider 持有。
 */
interface DirectingLocResult : DirectingResult {

    /**
     * 服务端完成校验后确认的落点。
     */
    val location: Location
}
