package com.gitee.planners.api.directing

import org.bukkit.entity.Entity

/**
 * 以单个实体为结果的指向确认结果。
 *
 * Planners 只通过该子类型把确认结果绑定到动作上下文的目标上，使 `@target` 目标器与
 * `<target.*>` 查询根读到同一个人；provider 自己的结果类型仍由 provider 持有，
 * Planners 不关心其具体实现。
 */
interface DirectingEntityResult : DirectingResult {

    /**
     * 服务端完成校验后确认的目标实体。
     */
    val entity: Entity
}
