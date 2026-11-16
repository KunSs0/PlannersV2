package com.gitee.planners.api.pick

import org.bukkit.entity.Entity

/**
 * 以单个实体为结果的指向确认结果。
 *
 * Planners 只通过该结果把确认目标绑定到动作上下文，使 `@target` 目标器与 `<target.*>`
 * 查询根读到同一个人。
 */
class PickEntityResult(val entity: Entity) : PickResult {

    override val type = EntityPickDefinition.TYPE
}
