package com.gitee.planners.api.common.util

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.util.BoundingBox

/**
 * 球形 / 圆柱选区实体查找器。
 *
 * 以 origin 为中心，按实体真实碰撞箱判定：给定 [halfHeight] 时按水平半径 + 垂直半高的圆柱判定，
 * 否则按球形半径判定。与其它选区共用同一套 AABB 与遮挡判定。
 *
 * @param origin 选区中心。
 * @param radius 球形半径或圆柱水平半径。
 * @param halfHeight 圆柱垂直半高；球形选区传 null。
 * @param samples 待筛选实体列表。
 */
class RangeNearestEntityFinder(
    origin: Location,
    val radius: Double,
    val halfHeight: Double?,
    samples: List<Entity>,
) : NearestEntityFinder(origin, samples) {

    override fun touches(box: BoundingBox): Boolean {
        val height = halfHeight
        if (height == null) {
            return SelectionGeometry.sphereTouchesBox(origin.x, origin.y, origin.z, radius, box)
        }
        return SelectionGeometry.cylinderTouchesBox(origin.x, origin.y, origin.z, radius, height, box)
    }

    override fun samplingBounds(): BoundingBox {
        val height = halfHeight
        if (height == null) {
            return SelectionGeometry.sphereBounds(origin.x, origin.y, origin.z, radius)
        }
        return SelectionGeometry.cylinderBounds(origin.x, origin.y, origin.z, radius, height)
    }
}
