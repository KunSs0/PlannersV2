package com.gitee.planners.api.common.util

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.util.BoundingBox
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D 矩形（Box）实体查找器。
 *
 * 以 origin + offset 为中心、沿 facing 方向旋转的矩形盒，判定实体真实碰撞箱是否与矩形相交：
 * 身体擦过矩形边缘即命中，不再要求实体中心进入矩形。
 *
 * @param origin 技能原点。
 * @param width 矩形宽度（左右方向，full）。
 * @param height 矩形高度（上下方向，full）。
 * @param length 矩形长度（前后方向，full）。
 * @param direction 朝向 yaw（度）。
 * @param offsetX 中心左右偏移（局部坐标，右为正）。
 * @param offsetY 中心上下偏移（世界 Y 轴）。
 * @param offsetZ 中心前后偏移（局部坐标，前为正）。
 * @param samples 待筛选实体列表。
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
    samples: List<Entity>,
) : NearestEntityFinder(origin, samples) {

    private val radians = Math.toRadians(direction.toDouble())
    private val forwardX = -sin(radians)
    private val forwardZ = cos(radians)
    private val rightX = cos(radians)
    private val rightZ = sin(radians)

    /**
     * 矩形中心 X。
     */
    val centerX = origin.x + offsetZ * forwardX + offsetX * rightX

    /**
     * 矩形中心 Y。
     */
    val centerY = origin.y + offsetY

    /**
     * 矩形中心 Z。
     */
    val centerZ = origin.z + offsetZ * forwardZ + offsetX * rightZ

    private val halfW = width / 2.0
    private val halfH = height / 2.0
    private val halfL = length / 2.0

    override fun request(): List<Entity> {
        val result = ArrayList<Entity>(samples.size)
        for (entity in samples) {
            val box = getBoundingBox(entity)
            val touched = SelectionGeometry.rotatedBoxTouchesBox(
                centerX,
                centerY,
                centerZ,
                halfW,
                halfH,
                halfL,
                forwardX,
                forwardZ,
                rightX,
                rightZ,
                box,
            )
            if (touched) {
                result.add(entity)
            }
        }
        return result
    }

    override fun samplingBounds(): BoundingBox {
        // 旋转矩形的世界 AABB 由 8 个角决定，先取角点再求各轴极值。
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var minZ = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        var maxZ = -Double.MAX_VALUE
        for (side in SIGNS) {
            for (vertical in SIGNS) {
                for (depth in SIGNS) {
                    val pointX = centerX + rightX * halfW * side + forwardX * halfL * depth
                    val pointY = centerY + halfH * vertical
                    val pointZ = centerZ + rightZ * halfW * side + forwardZ * halfL * depth
                    if (pointX < minX) {
                        minX = pointX
                    }
                    if (pointX > maxX) {
                        maxX = pointX
                    }
                    if (pointY < minY) {
                        minY = pointY
                    }
                    if (pointY > maxY) {
                        maxY = pointY
                    }
                    if (pointZ < minZ) {
                        minZ = pointZ
                    }
                    if (pointZ > maxZ) {
                        maxZ = pointZ
                    }
                }
            }
        }
        return BoundingBox(minX, minY, minZ, maxX, maxY, maxZ)
    }

    companion object {

        /**
         * 角点符号组合。
         */
        private val SIGNS = doubleArrayOf(-1.0, 1.0)
    }
}
