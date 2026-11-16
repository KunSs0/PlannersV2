package com.gitee.planners.api.common.util

import org.bukkit.util.BoundingBox
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * 选区形状与实体碰撞箱的相交判定。
 *
 * 所有选区（矩形、扇形、球形、圆柱）都以实体真实 AABB 为判定对象：只比较实体位置点会让
 * 身体贴边的目标漏判，按实体宽度无条件放大形状又会破坏形状自身的边界。本对象只做纯几何
 * 计算，不读取 Bukkit 实体状态，便于单元测试和复用。
 */
object SelectionGeometry {

    /**
     * 浮点比较容差，避免恰好贴在边界上的实体因舍入被判为未命中。
     */
    private const val EPSILON = 1.0e-9

    /**
     * 判定绕 Y 轴旋转的矩形盒是否与碰撞箱相交。
     *
     * 把碰撞箱的半尺寸投影到矩形三条局部轴上做区间重叠，三条轴都重叠即认为相交。
     * 对“角点擦过棱边”的极端姿态可能略微放宽，但不会漏判，符合命中查询的取向。
     *
     * @param centerX 矩形中心 X。
     * @param centerY 矩形中心 Y。
     * @param centerZ 矩形中心 Z。
     * @param halfWidth 矩形左右半宽。
     * @param halfHeight 矩形上下半高。
     * @param halfLength 矩形前后半长。
     * @param forwardX 前方单位向量的 X 分量。
     * @param forwardZ 前方单位向量的 Z 分量。
     * @param rightX 右方单位向量的 X 分量。
     * @param rightZ 右方单位向量的 Z 分量。
     * @param box 实体碰撞箱。
     * @return 相交时返回 true。
     */
    fun rotatedBoxTouchesBox(
        centerX: Double,
        centerY: Double,
        centerZ: Double,
        halfWidth: Double,
        halfHeight: Double,
        halfLength: Double,
        forwardX: Double,
        forwardZ: Double,
        rightX: Double,
        rightZ: Double,
        box: BoundingBox,
    ): Boolean {
        val boxHalfX = (box.maxX - box.minX) / 2.0
        val boxHalfY = (box.maxY - box.minY) / 2.0
        val boxHalfZ = (box.maxZ - box.minZ) / 2.0
        val deltaX = (box.minX + box.maxX) / 2.0 - centerX
        val deltaY = (box.minY + box.maxY) / 2.0 - centerY
        val deltaZ = (box.minZ + box.maxZ) / 2.0 - centerZ

        // 上下轴：只有世界 Y 参与投影。
        if (abs(deltaY) > halfHeight + boxHalfY + EPSILON) {
            return false
        }

        // 左右轴：世界 X/Z 各自按右方单位向量的分量贡献半尺寸。
        val localX = deltaX * rightX + deltaZ * rightZ
        val extentX = boxHalfX * abs(rightX) + boxHalfZ * abs(rightZ)
        if (abs(localX) > halfWidth + extentX + EPSILON) {
            return false
        }

        // 前后轴：世界 X/Z 各自按前方单位向量的分量贡献半尺寸。
        val localZ = deltaX * forwardX + deltaZ * forwardZ
        val extentZ = boxHalfX * abs(forwardX) + boxHalfZ * abs(forwardZ)
        if (abs(localZ) > halfLength + extentZ + EPSILON) {
            return false
        }
        return true
    }

    /**
     * 判定水平扇形是否与碰撞箱相交。
     *
     * 扇形是“顶点 + 半径 + 半开角”的二维区域，垂直方向不参与判定，与既有 `@sector` 语义一致。
     * 判定方式是求顶点到「碰撞箱水平投影 ∩ 扇形」的最近距离：最近点只会落在碰撞箱角点、
     * 顶点射线与碰撞箱边的交点、或碰撞箱边的垂足上，因此取这三类候选点的最小值即可。
     *
     * @param apexX 扇形顶点 X。
     * @param apexZ 扇形顶点 Z。
     * @param radius 扇形半径。
     * @param halfAngle 扇形半开角（弧度）。
     * @param forwardX 扇形中轴单位向量的 X 分量。
     * @param forwardZ 扇形中轴单位向量的 Z 分量。
     * @param box 实体碰撞箱。
     * @return 相交时返回 true。
     */
    fun sectorTouchesBox(
        apexX: Double,
        apexZ: Double,
        radius: Double,
        halfAngle: Double,
        forwardX: Double,
        forwardZ: Double,
        box: BoundingBox,
    ): Boolean {
        if (radius <= 0.0) {
            return false
        }
        val minX = box.minX
        val maxX = box.maxX
        val minZ = box.minZ
        val maxZ = box.maxZ

        // 顶点落在碰撞箱水平投影内：顶点同时属于两者，必然相交。
        if (apexX >= minX && apexX <= maxX && apexZ >= minZ && apexZ <= maxZ) {
            return true
        }

        // 开角达到整圈时扇形退化为圆，直接比较圆与矩形的最近距离。
        if (halfAngle >= Math.PI - EPSILON) {
            return horizontalDistanceSquared(apexX, apexZ, minX, maxX, minZ, maxZ) <= radius * radius + EPSILON
        }

        var best = Double.MAX_VALUE

        // 候选一：碰撞箱四条水平边的垂足。最近点可能落在边的中部而不是角点上。
        if (apexZ >= minZ && apexZ <= maxZ) {
            best = minOf(best, pointDistanceSquared(apexX, apexZ, forwardX, forwardZ, halfAngle, minX, apexZ))
            best = minOf(best, pointDistanceSquared(apexX, apexZ, forwardX, forwardZ, halfAngle, maxX, apexZ))
        }
        if (apexX >= minX && apexX <= maxX) {
            best = minOf(best, pointDistanceSquared(apexX, apexZ, forwardX, forwardZ, halfAngle, apexX, minZ))
            best = minOf(best, pointDistanceSquared(apexX, apexZ, forwardX, forwardZ, halfAngle, apexX, maxZ))
        }

        // 候选二：落进扇形的碰撞箱角点。
        best = minOf(best, pointDistanceSquared(apexX, apexZ, forwardX, forwardZ, halfAngle, minX, minZ))
        best = minOf(best, pointDistanceSquared(apexX, apexZ, forwardX, forwardZ, halfAngle, minX, maxZ))
        best = minOf(best, pointDistanceSquared(apexX, apexZ, forwardX, forwardZ, halfAngle, maxX, minZ))
        best = minOf(best, pointDistanceSquared(apexX, apexZ, forwardX, forwardZ, halfAngle, maxX, maxZ))

        // 候选三：扇形两条边与碰撞箱的交点。
        val rotation = cos(halfAngle)
        val chord = sin(halfAngle)
        val leftX = forwardX * rotation - forwardZ * chord
        val leftZ = forwardX * chord + forwardZ * rotation
        val rightX = forwardX * rotation + forwardZ * chord
        val rightZ = -forwardX * chord + forwardZ * rotation
        best = minOf(best, rayEntrySquared(leftX, leftZ, apexX, apexZ, minX, maxX, minZ, maxZ))
        best = minOf(best, rayEntrySquared(rightX, rightZ, apexX, apexZ, minX, maxX, minZ, maxZ))

        return best <= radius * radius + EPSILON
    }

    /**
     * 判定球形选区是否与碰撞箱相交。
     *
     * @param centerX 球心 X。
     * @param centerY 球心 Y。
     * @param centerZ 球心 Z。
     * @param radius 球半径。
     * @param box 实体碰撞箱。
     * @return 相交时返回 true。
     */
    fun sphereTouchesBox(centerX: Double, centerY: Double, centerZ: Double, radius: Double, box: BoundingBox): Boolean {
        if (radius <= 0.0) {
            return false
        }
        val deltaX = axisDistance(centerX, box.minX, box.maxX)
        val deltaY = axisDistance(centerY, box.minY, box.maxY)
        val deltaZ = axisDistance(centerZ, box.minZ, box.maxZ)
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ <= radius * radius + EPSILON
    }

    /**
     * 判定圆柱选区是否与碰撞箱相交。
     *
     * 水平方向按半径判定，垂直方向按中心上下半高判定，避免球形判定在高低差场景误放行。
     *
     * @param centerX 圆柱轴心 X。
     * @param centerY 圆柱轴心 Y。
     * @param centerZ 圆柱轴心 Z。
     * @param radius 水平半径。
     * @param halfHeight 垂直半高。
     * @param box 实体碰撞箱。
     * @return 相交时返回 true。
     */
    fun cylinderTouchesBox(
        centerX: Double,
        centerY: Double,
        centerZ: Double,
        radius: Double,
        halfHeight: Double,
        box: BoundingBox,
    ): Boolean {
        if (radius <= 0.0 || halfHeight <= 0.0) {
            return false
        }
        val deltaX = axisDistance(centerX, box.minX, box.maxX)
        val deltaZ = axisDistance(centerZ, box.minZ, box.maxZ)
        if (deltaX * deltaX + deltaZ * deltaZ > radius * radius + EPSILON) {
            return false
        }
        if (box.maxY < centerY - halfHeight - EPSILON) {
            return false
        }
        if (box.minY > centerY + halfHeight + EPSILON) {
            return false
        }
        return true
    }

    /**
     * 取球形选区的取样包围盒。
     *
     * @param centerX 球心 X。
     * @param centerY 球心 Y。
     * @param centerZ 球心 Z。
     * @param radius 球半径。
     * @return 覆盖整个球的世界轴对齐包围盒。
     */
    fun sphereBounds(centerX: Double, centerY: Double, centerZ: Double, radius: Double): BoundingBox {
        return BoundingBox(
            centerX - radius,
            centerY - radius,
            centerZ - radius,
            centerX + radius,
            centerY + radius,
            centerZ + radius,
        )
    }

    /**
     * 取圆柱选区的取样包围盒。
     *
     * @param centerX 轴心 X。
     * @param centerY 轴心 Y。
     * @param centerZ 轴心 Z。
     * @param radius 水平半径。
     * @param halfHeight 垂直半高。
     * @return 覆盖整个圆柱的世界轴对齐包围盒。
     */
    fun cylinderBounds(
        centerX: Double,
        centerY: Double,
        centerZ: Double,
        radius: Double,
        halfHeight: Double,
    ): BoundingBox {
        return BoundingBox(
            centerX - radius,
            centerY - halfHeight,
            centerZ - radius,
            centerX + radius,
            centerY + halfHeight,
            centerZ + radius,
        )
    }

    /**
     * 计算候选点（垂足或角点）到扇形顶点的距离平方。
     *
     * @param apexX 扇形顶点 X。
     * @param apexZ 扇形顶点 Z。
     * @param forwardX 中轴单位向量 X 分量。
     * @param forwardZ 中轴单位向量 Z 分量。
     * @param halfAngle 扇形半开角（弧度）。
     * @param pointX 候选点 X。
     * @param pointZ 候选点 Z。
     * @return 候选点在扇形内时返回距离平方，否则返回最大值。
     */
    private fun pointDistanceSquared(
        apexX: Double,
        apexZ: Double,
        forwardX: Double,
        forwardZ: Double,
        halfAngle: Double,
        pointX: Double,
        pointZ: Double,
    ): Double {
        if (!insideWedge(apexX, apexZ, forwardX, forwardZ, halfAngle, pointX, pointZ)) {
            return Double.MAX_VALUE
        }
        val deltaX = pointX - apexX
        val deltaZ = pointZ - apexZ
        return deltaX * deltaX + deltaZ * deltaZ
    }

    /**
     * 判断点是否落在扇形开角内。
     *
     * @param apexX 扇形顶点 X。
     * @param apexZ 扇形顶点 Z。
     * @param forwardX 中轴单位向量 X 分量。
     * @param forwardZ 中轴单位向量 Z 分量。
     * @param halfAngle 扇形半开角（弧度）。
     * @param pointX 待判定点 X。
     * @param pointZ 待判定点 Z。
     * @return 在开角内时返回 true。
     */
    private fun insideWedge(
        apexX: Double,
        apexZ: Double,
        forwardX: Double,
        forwardZ: Double,
        halfAngle: Double,
        pointX: Double,
        pointZ: Double,
    ): Boolean {
        val deltaX = pointX - apexX
        val deltaZ = pointZ - apexZ
        if (abs(deltaX) <= EPSILON && abs(deltaZ) <= EPSILON) {
            return true
        }
        // 用叉积与点积求带符号夹角，再与半开角比较；符号方向无关紧要，因为开角对称。
        val twoArea = forwardX * deltaZ - forwardZ * deltaX
        val dot = forwardX * deltaX + forwardZ * deltaZ
        val difference = atan2(twoArea, dot)
        return abs(difference) <= halfAngle + EPSILON
    }

    /**
     * 计算从顶点沿给定方向射出后，进入碰撞箱时的距离平方。
     *
     * @param directionX 射线方向 X 分量。
     * @param directionZ 射线方向 Z 分量。
     * @param apexX 顶点 X。
     * @param apexZ 顶点 Z。
     * @param minX 碰撞箱最小 X。
     * @param maxX 碰撞箱最大 X。
     * @param minZ 碰撞箱最小 Z。
     * @param maxZ 碰撞箱最大 Z。
     * @return 进入点距离平方；射线不经过碰撞箱时返回最大值。
     */
    private fun rayEntrySquared(
        directionX: Double,
        directionZ: Double,
        apexX: Double,
        apexZ: Double,
        minX: Double,
        maxX: Double,
        minZ: Double,
        maxZ: Double,
    ): Double {
        var enter = 0.0
        var exit = Double.MAX_VALUE
        if (abs(directionX) <= EPSILON) {
            if (apexX < minX || apexX > maxX) {
                return Double.MAX_VALUE
            }
        } else {
            val first = (minX - apexX) / directionX
            val second = (maxX - apexX) / directionX
            enter = max(enter, min(first, second))
            exit = min(exit, max(first, second))
        }
        if (abs(directionZ) <= EPSILON) {
            if (apexZ < minZ || apexZ > maxZ) {
                return Double.MAX_VALUE
            }
        } else {
            val first = (minZ - apexZ) / directionZ
            val second = (maxZ - apexZ) / directionZ
            enter = max(enter, min(first, second))
            exit = min(exit, max(first, second))
        }
        if (enter > exit) {
            return Double.MAX_VALUE
        }
        // 射线只取正向部分，因此进入参数即距离。
        return enter * enter
    }

    /**
     * 计算顶点到矩形最近水平距离的平方。
     *
     * @param pointX 顶点 X。
     * @param pointZ 顶点 Z。
     * @param minX 矩形最小 X。
     * @param maxX 矩形最大 X。
     * @param minZ 矩形最小 Z。
     * @param maxZ 矩形最大 Z。
     * @return 距离平方。
     */
    private fun horizontalDistanceSquared(
        pointX: Double,
        pointZ: Double,
        minX: Double,
        maxX: Double,
        minZ: Double,
        maxZ: Double,
    ): Double {
        val deltaX = axisDistance(pointX, minX, maxX)
        val deltaZ = axisDistance(pointZ, minZ, maxZ)
        return deltaX * deltaX + deltaZ * deltaZ
    }

    /**
     * 计算单轴到区间的距离。
     *
     * @param value 坐标。
     * @param min 区间下界。
     * @param max 区间上界。
     * @return 落在区间内返回 0，否则返回到最近边界的距离。
     */
    private fun axisDistance(value: Double, min: Double, max: Double): Double {
        if (value < min) {
            return min - value
        }
        if (value > max) {
            return value - max
        }
        return 0.0
    }
}
