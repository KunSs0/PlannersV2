package com.gitee.planners.api.attribute

import org.bukkit.entity.LivingEntity

/**
 * 属性来源接口。
 * 任意模块实现此接口并调用 [com.gitee.planners.core.attribute.AttributeProxy.register]
 * 即可向属性管线贡献属性值。
 */
interface AttributeSource {

    /** 来源标识 */
    val id: String

    /** 优先级，越小越先计算。同名 key 累加 */
    val priority: Int

    /**
     * 返回该来源提供的属性。
     *
     * 键为当前属性驱动所属属性系统的物理属性 ID，例如 FightCore 的 `strength`、`health-regen`。
     * 同一个键由多个来源提供时按来源顺序累加。
     */
    fun getAttributes(entity: LivingEntity): Map<String, Double>

    companion object {
        const val PRIORITY_BASE = 0
        const val PRIORITY_SKILL = 5
        const val PRIORITY_GROWTH = 10
        const val PRIORITY_INVESTED = 20
        const val PRIORITY_EQUIP = 30
        const val PRIORITY_BUFF = 40
        const val PRIORITY_OVERRIDE = 100
    }
}
