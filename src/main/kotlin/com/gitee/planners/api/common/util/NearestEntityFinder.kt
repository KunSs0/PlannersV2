package com.gitee.planners.api.common.util

import org.bukkit.Location
import org.bukkit.entity.Entity
import taboolib.module.navigation.BoundingBox

abstract class NearestEntityFinder(val origin: Location, var samples: List<Entity>) {

    constructor(origin: Location,sampling: SynchronousSampling<List<Entity>>) : this(origin,sampling.get())

    abstract fun request(): List<Entity>

    /**
     * 读取实体当前的碰撞箱。
     *
     * 不缓存结果：实体会移动、被击退或改变尺寸，跨查询复用旧包围盒会让
     * 选区使用过期坐标，导致目标明明在范围内却选不中。
     */
    fun getBoundingBox(entity: Entity): BoundingBox {
        val boundingBoxWidth = entity.width
        val boundingBoxHeight = entity.height
        val x = entity.location.x
        val y = entity.location.y
        val z = entity.location.z
        return BoundingBox(
            x - boundingBoxWidth, y - boundingBoxHeight, z - boundingBoxWidth,
            x + boundingBoxWidth, y + boundingBoxHeight, z + boundingBoxWidth,
        )
    }

}
