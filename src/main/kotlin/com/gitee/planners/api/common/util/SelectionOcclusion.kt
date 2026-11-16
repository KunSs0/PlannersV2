package com.gitee.planners.api.common.util

import org.bukkit.FluidCollisionMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector

/**
 * 选区遮挡判定。
 *
 * 选区不允许穿过实心方块捕获目标：从选区原点向目标碰撞箱最近点投射一条射线，遇到第一个
 * 不属于白名单的方块即视为被遮挡。白名单方块（例如栅栏）会按自身碰撞形状求出出口后继续
 * 推进，因此“连续多道栅栏”可以通过、“栅栏后还有石墙”仍然被挡。
 *
 * 白名单来自服务端配置，属系统级规则；本对象不提供每技能开关，也不缓存世界状态。
 */
object SelectionOcclusion {

    /** 判定长度下限，短于此距离视为同点。 */
    private const val MIN_DISTANCE = 1.0e-6

    /** 穿过白名单方块后继续射线的推进余量，避免命中同一表面。 */
    private const val STEP = 1.0e-3

    /** 单条视线最多穿过的白名单方块数，超出按遮挡处理。 */
    private const val MAX_PASS = 32

    /**
     * 当前生效的穿透白名单；由配置加载写入。
     */
    private var passThrough: Set<Material> = emptySet()

    /**
     * 更新穿透白名单。
     *
     * @param materials 不遮挡视线的方块类型。
     */
    fun configure(materials: Collection<Material>) {
        val updated = LinkedHashSet<Material>()
        updated.addAll(materials)
        passThrough = updated
    }

    /**
     * 判断选区能否看到目标。
     *
     * @param world 目标所在世界。
     * @param from 选区原点（已抬到判定高度）。
     * @param target 目标碰撞箱。
     * @return 未被遮挡时返回 true。
     */
    fun isClear(world: World?, from: Location, target: BoundingBox): Boolean {
        if (world == null) {
            // 合成环境（Nova mock）没有世界，无法判定方块遮挡；生产路径的选区都会先校验世界。
            return true
        }
        val targetX = clamp(from.x, target.minX, target.maxX)
        val targetY = clamp(from.y, target.minY, target.maxY)
        val targetZ = clamp(from.z, target.minZ, target.maxZ)
        var startX = from.x
        var startY = from.y
        var startZ = from.z
        var deltaX = targetX - startX
        var deltaY = targetY - startY
        var deltaZ = targetZ - startZ
        var remaining = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
        if (remaining <= MIN_DISTANCE) {
            return true
        }
        var direction = Vector(deltaX / remaining, deltaY / remaining, deltaZ / remaining)
        var passed = 0
        while (remaining > MIN_DISTANCE) {
            val start = Location(world, startX, startY, startZ)
            val hit = world.rayTraceBlocks(start, direction, remaining, FluidCollisionMode.NEVER, true)
            if (hit == null) {
                return true
            }
            val block = hit.hitBlock
            if (block == null) {
                return true
            }
            if (!passThrough.contains(block.type)) {
                return false
            }
            val exit = exitDistance(block, startX, startY, startZ, direction)
            if (exit == null) {
                // 白名单方块却给不出出口距离：按遮挡处理，避免视线无声穿墙。
                return false
            }
            passed += 1
            if (passed > MAX_PASS) {
                return false
            }
            val advance = exit + STEP
            startX += direction.x * advance
            startY += direction.y * advance
            startZ += direction.z * advance
            remaining -= advance
            deltaX = targetX - startX
            deltaY = targetY - startY
            deltaZ = targetZ - startZ
            val next = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
            if (next <= MIN_DISTANCE) {
                return true
            }
            direction = Vector(deltaX / next, deltaY / next, deltaZ / next)
            remaining = next
        }
        return true
    }

    /**
     * 求射线离开方块碰撞形状的距离。
     *
     * 栅栏、栅栏门等方块的碰撞形状不是整格，必须按形状算出口，否则会停在形状内部反复命中。
     *
     * @param block 白名单方块。
     * @param startX 射线起点 X。
     * @param startY 射线起点 Y。
     * @param startZ 射线起点 Z。
     * @param direction 射线方向（单位向量）。
     * @return 出口距离；形状与射线无交集时返回 null。
     */
    private fun exitDistance(block: Block, startX: Double, startY: Double, startZ: Double, direction: Vector): Double? {
        var furthest = null as Double?
        val boxes = block.collisionShape.boundingBoxes
        for (box in boxes) {
            val exit = exitDistanceOfBox(box, startX, startY, startZ, direction)
            if (exit == null || exit <= 0.0) {
                // 起点已经越过该碰撞形状：不参与“出口”计算，避免原地打转。
                continue
            }
            if (furthest == null || exit > furthest) {
                furthest = exit
            }
        }
        return furthest
    }

    /**
     * 求射线离开单个碰撞箱的距离。
     *
     * @param box 碰撞箱。
     * @param startX 射线起点 X。
     * @param startY 射线起点 Y。
     * @param startZ 射线起点 Z。
     * @param direction 射线方向（单位向量）。
     * @return 出口距离；射线不穿过该箱时返回 null。
     */
    private fun exitDistanceOfBox(
        box: BoundingBox,
        startX: Double,
        startY: Double,
        startZ: Double,
        direction: Vector,
    ): Double? {
        var enter = -Double.MAX_VALUE
        var exit = Double.MAX_VALUE
        val xRange = axisRange(startX, direction.x, box.minX, box.maxX)
        if (xRange == null) {
            return null
        }
        enter = Math.max(enter, xRange[0])
        exit = Math.min(exit, xRange[1])
        val yRange = axisRange(startY, direction.y, box.minY, box.maxY)
        if (yRange == null) {
            return null
        }
        enter = Math.max(enter, yRange[0])
        exit = Math.min(exit, yRange[1])
        val zRange = axisRange(startZ, direction.z, box.minZ, box.maxZ)
        if (zRange == null) {
            return null
        }
        enter = Math.max(enter, zRange[0])
        exit = Math.min(exit, zRange[1])
        if (enter > exit) {
            return null
        }
        return exit
    }

    /**
     * 求射线在一个轴向上的参数区间。
     *
     * @param start 起点坐标。
     * @param direction 该轴方向分量。
     * @param min 区间下界。
     * @param max 区间上界。
     * @return 参数区间；与该轴平行且不在区间内时返回 null。
     */
    private fun axisRange(start: Double, direction: Double, min: Double, max: Double): DoubleArray? {
        if (Math.abs(direction) < MIN_DISTANCE) {
            if (start < min || start > max) {
                return null
            }
            return doubleArrayOf(-Double.MAX_VALUE, Double.MAX_VALUE)
        }
        val first = (min - start) / direction
        val second = (max - start) / direction
        if (first < second) {
            return doubleArrayOf(first, second)
        }
        return doubleArrayOf(second, first)
    }

    /**
     * 把坐标夹到区间内。
     *
     * @param value 坐标。
     * @param min 区间下界。
     * @param max 区间上界。
     * @return 夹取结果。
     */
    private fun clamp(value: Double, min: Double, max: Double): Double {
        if (value < min) {
            return min
        }
        if (value > max) {
            return max
        }
        return value
    }
}
