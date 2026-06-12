package com.gitee.planners.api.common.util

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.util.Vector
import taboolib.module.navigation.BoundingBox
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D 矩形（Box）实体查找器
 *
 * 以 origin + offset 为中心，沿 facing 方向旋转的矩形包围盒，
 * 检测实体包围盒中心是否在矩形内。
 *
 * @param origin    技能原点
 * @param width     矩形宽度（左右方向，full）
 * @param height    矩形高度（上下方向，full）
 * @param length    矩形长度（前后方向，full）
 * @param direction 朝向 yaw（度）
 * @param offsetX   中心左右偏移（局部坐标，右为正）
 * @param offsetY   中心上下偏移（世界 Y 轴）
 * @param offsetZ   中心前后偏移（局部坐标，前为正）
 * @param samples   待筛选实体列表
 */
class RectNearestEntityFinder(
    origin: Location,
    val width: Double,
    val height: Double,
    val length: Double,
    val direction: Float,
    val offsetX: Double,
    val offsetY: Double,
    val offsetZ: Double,
    samples: List<Entity>
) : NearestEntityFinder(origin, samples) {

    private val radians = Math.toRadians(direction.toDouble())
    private val forwardX = -sin(radians)
    private val forwardZ = cos(radians)
    private val rightX = cos(radians)
    private val rightZ = sin(radians)

    val centerX = origin.x + offsetZ * forwardX + offsetX * rightX
    val centerY = origin.y + offsetY
    val centerZ = origin.z + offsetZ * forwardZ + offsetX * rightZ

    private val halfW = width / 2.0
    private val halfH = height / 2.0
    private val halfL = length / 2.0

    override fun request(): List<Entity> {
        return samples.filter { isBoundingBoxInRect(getBoundingBox(it)) }
    }

    private fun isBoundingBoxInRect(bb: BoundingBox): Boolean {
        val bc = bb.getCenter()

        // 实体包围盒中心 → rect 中心的向量
        val dx = bc.x - centerX
        val dy = bc.y - centerY
        val dz = bc.z - centerZ

        // 投影到 rect 局部坐标系
        val localX = dx * rightX + dz * rightZ    // 左右分量
        val localY = dy                             // 上下分量（世界 Y 轴）
        val localZ = dx * forwardX + dz * forwardZ  // 前后分量

        return abs(localX) <= halfW &&
                abs(localY) <= halfH &&
                abs(localZ) <= halfL
    }

    private fun BoundingBox.getCenter(): Vector {
        return Vector((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2)
    }
}
