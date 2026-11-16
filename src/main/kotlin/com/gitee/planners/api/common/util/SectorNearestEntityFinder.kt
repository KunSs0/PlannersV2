package com.gitee.planners.api.common.util

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.util.BoundingBox
import kotlin.math.cos
import kotlin.math.sin

/**
 * 水平扇形实体查找器。
 *
 * 以 origin 为顶点、沿 facing 方向张开 angle 度的扇形，判定实体真实碰撞箱是否与扇形相交：
 * 身体压住扇面边缘即命中，不再要求实体中心落在扇形内。垂直方向不参与判定，与既有语义一致。
 *
 * @param origin 扇形顶点。
 * @param angle 扇形开角（度，full）。
 * @param radius 扇形半径。
 * @param direction 朝向 yaw（度）。
 * @param samples 待筛选实体列表。
 */
class SectorNearestEntityFinder(
    origin: Location,
    val angle: Double,
    val radius: Double,
    val direction: Float,
    samples: List<Entity>,
) : NearestEntityFinder(origin, samples) {

    private val radians = Math.toRadians(direction.toDouble())
    private val forwardX = -sin(radians)
    private val forwardZ = cos(radians)
    private val halfAngle = Math.toRadians(angle / 2.0)

    override fun touches(box: BoundingBox): Boolean {
        return SelectionGeometry.sectorTouchesBox(
            origin.x,
            origin.z,
            radius,
            halfAngle,
            forwardX,
            forwardZ,
            box,
        )
    }

    override fun samplingBounds(): BoundingBox {
        // 扇形始终落在“顶点 + 半径”的圆内，用该圆的外接立方体取样即可覆盖全部命中。
        return SelectionGeometry.sphereBounds(origin.x, origin.y, origin.z, radius)
    }
}
