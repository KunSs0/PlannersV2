package com.gitee.planners.module.action

import com.gitee.planners.api.common.util.RangeNearestEntityFinder
import com.gitee.planners.api.common.util.RectNearestEntityFinder
import com.gitee.planners.api.common.util.SectorNearestEntityFinder
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.entity.Player
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 选区调试绘制。
 *
 * 开启后，内置选区目标器在查询前把本次判定范围按采样间距绘成粒子，只发送给施法玩家本人。
 * 矩形画三维棱框，扇形画平面两条边与圆弧，球形画三个正交大圆，圆柱画上下两个圆与四条母线。
 * 几何参数全部直接读取对应查找器的公开值，因此画出来的范围与实际参与判定的范围一致。
 *
 * 本对象只做绘制，不参与命中判定；开关关闭时目标器不会调用这里，也不产生任何粒子包。
 */
object SelectorDebug {

    /** 水平圆所在平面：上下圆与球形横切圆都使用它。 */
    private const val PLANE_HORIZONTAL = 0

    /** 竖直圆所在平面：法线为 Z 轴，用于球形调试。 */
    private const val PLANE_VERTICAL_Z = 1

    /** 竖直圆所在平面：法线为 X 轴，用于球形调试。 */
    private const val PLANE_VERTICAL_X = 2

    /** 判断符号组合，同时服务角点与上下取值。 */
    private val SIGNS = doubleArrayOf(-1.0, 1.0)

    private var enabled = false

    private var particle = Particle.END_ROD

    private var step = 0.5

    private var verticalOffset = 0.15

    /**
     * 应用一次配置快照。
     *
     * 由插件在启动与重载后调用；采样间距与抬高量不合法时直接报错，
     * 不做静默纠偏，避免配置写错却画出错误范围。
     *
     * @param enabled 是否开启选区调试绘制。
     * @param particle 绘制使用的粒子类型。
     * @param step 采样间距，单位格。
     * @param verticalOffset 扇形平面的抬高量，单位格。
     */
    fun apply(enabled: Boolean, particle: Particle, step: Double, verticalOffset: Double) {
        if (!step.isFinite() || step <= 0.0) {
            throw IllegalArgumentException("settings.debug.selector.step 必须为正有限值，当前为 $step")
        }
        if (!verticalOffset.isFinite()) {
            throw IllegalArgumentException("settings.debug.selector.y-offset 必须为有限值，当前为 $verticalOffset")
        }
        this.enabled = enabled
        this.particle = particle
        this.step = step
        this.verticalOffset = verticalOffset
    }

    /**
     * 当前是否开启选区调试绘制。
     *
     * @return 开启时返回 true。
     */
    fun isEnabled(): Boolean {
        return enabled
    }

    /**
     * 绘制矩形选区的三维棱框。
     *
     * @param sender 本次执行的施法者；不是玩家时不绘制，避免把调试框广播给其他玩家。
     * @param finder 已按动作参数构造的矩形查找器。
     */
    fun drawRect(sender: Any?, finder: RectNearestEntityFinder) {
        if (!enabled) {
            return
        }
        val player = viewerOf(sender)
        if (player == null) {
            return
        }
        val world = finder.origin.world
        if (world == null) {
            return
        }
        for (point in rectPoints(world, finder, step)) {
            spawn(player, point)
        }
    }

    /**
     * 绘制扇形选区的两条边与圆弧。
     *
     * 扇形判定只看水平方向，因此整个图形画在原点抬高的同一平面上。
     *
     * @param sender 本次执行的施法者；不是玩家时不绘制。
     * @param finder 已按动作参数构造的扇形查找器。
     */
    fun drawSector(sender: Any?, finder: SectorNearestEntityFinder) {
        if (!enabled) {
            return
        }
        val player = viewerOf(sender)
        if (player == null) {
            return
        }
        val world = finder.origin.world
        if (world == null) {
            return
        }
        for (point in sectorPoints(world, finder, step, verticalOffset)) {
            spawn(player, point)
        }
    }

    /**
     * 绘制球形或圆柱选区。
     *
     * @param sender 本次执行的施法者；不是玩家时不绘制。
     * @param finder 已按动作参数构造的球形/圆柱查找器。
     */
    fun drawRange(sender: Any?, finder: RangeNearestEntityFinder) {
        if (!enabled) {
            return
        }
        val player = viewerOf(sender)
        if (player == null) {
            return
        }
        val world = finder.origin.world
        if (world == null) {
            return
        }
        for (point in rangePoints(world, finder, step)) {
            spawn(player, point)
        }
    }

    /**
     * 计算矩形盒 12 条棱的采样点。
     *
     * 中心、半尺寸与朝向公式与 [RectNearestEntityFinder] 完全一致：左右按右方向轴、
     * 上下按世界 Y 轴、前后按前方向轴展开，再用 [step] 对每条棱均匀取点。
     *
     * @param world 粒子所在世界。
     * @param finder 矩形查找器。
     * @param step 采样间距，单位格。
     * @return 覆盖全部棱的采样点。
     */
    fun rectPoints(world: World, finder: RectNearestEntityFinder, step: Double): List<Location> {
        val radians = Math.toRadians(finder.direction.toDouble())
        val forwardX = -sin(radians)
        val forwardZ = cos(radians)
        val rightX = cos(radians)
        val rightZ = sin(radians)
        val halfWidth = finder.width / 2.0
        val halfHeight = finder.height / 2.0
        val halfLength = finder.length / 2.0
        val result = ArrayList<Location>()

        // 4 条垂直棱：左右与前后符号固定，Y 从下到上。
        for (side in SIGNS) {
            for (depth in SIGNS) {
                val x = finder.centerX + rightX * halfWidth * side + forwardX * halfLength * depth
                val z = finder.centerZ + rightZ * halfWidth * side + forwardZ * halfLength * depth
                appendLine(
                    result,
                    world,
                    x,
                    finder.centerY - halfHeight,
                    z,
                    x,
                    finder.centerY + halfHeight,
                    z,
                    step,
                )
            }
        }

        // 4 条前后棱：左右与上下符号固定，沿前方轴从近端到远端。
        for (side in SIGNS) {
            for (vertical in SIGNS) {
                val y = finder.centerY + halfHeight * vertical
                val nearX = finder.centerX + rightX * halfWidth * side - forwardX * halfLength
                val nearZ = finder.centerZ + rightZ * halfWidth * side - forwardZ * halfLength
                val farX = finder.centerX + rightX * halfWidth * side + forwardX * halfLength
                val farZ = finder.centerZ + rightZ * halfWidth * side + forwardZ * halfLength
                appendLine(result, world, nearX, y, nearZ, farX, y, farZ, step)
            }
        }

        // 4 条左右棱：上下与前后符号固定，沿右方向轴从左侧到右侧。
        for (vertical in SIGNS) {
            for (depth in SIGNS) {
                val y = finder.centerY + halfHeight * vertical
                val leftX = finder.centerX - rightX * halfWidth + forwardX * halfLength * depth
                val leftZ = finder.centerZ - rightZ * halfWidth + forwardZ * halfLength * depth
                val rightCornerX = finder.centerX + rightX * halfWidth + forwardX * halfLength * depth
                val rightCornerZ = finder.centerZ + rightZ * halfWidth + forwardZ * halfLength * depth
                appendLine(result, world, leftX, y, leftZ, rightCornerX, y, rightCornerZ, step)
            }
        }
        return result
    }

    /**
     * 计算扇形选区的采样点。
     *
     * 两条边沿开角边界取点，圆弧按弧长换算角度步长（最小 1 度），
     * 绘制平面位于原点上方 [verticalOffset] 格。
     *
     * @param world 粒子所在世界。
     * @param finder 扇形查找器。
     * @param step 采样间距，单位格。
     * @param verticalOffset 绘制平面的抬高量，单位格。
     * @return 两条边与圆弧的采样点。
     */
    fun sectorPoints(
        world: World,
        finder: SectorNearestEntityFinder,
        step: Double,
        verticalOffset: Double,
    ): List<Location> {
        val apex = finder.origin
        val planeY = apex.y + verticalOffset
        val halfAngle = finder.angle / 2.0
        val leftYaw = finder.direction - halfAngle
        val rightYaw = finder.direction + halfAngle
        val result = ArrayList<Location>()
        appendRadial(result, world, apex.x, planeY, apex.z, leftYaw, finder.radius, step)
        appendRadial(result, world, apex.x, planeY, apex.z, rightYaw, finder.radius, step)
        // 圆弧步长按弧长换算成角度，半径很小时退回 1 度，避免把整条弧压缩成一个点。
        val arcStepDegrees = Math.toDegrees(step / finder.radius)
        val arcStep = if (arcStepDegrees < 1.0) 1.0 else arcStepDegrees
        val segments = ceil(finder.angle / arcStep).toInt()
        val safeSegments = if (segments < 1) 1 else segments
        for (index in 0..safeSegments) {
            val yaw = leftYaw + finder.angle * index / safeSegments
            result.add(radialPoint(world, apex.x, planeY, apex.z, yaw, finder.radius))
        }
        return result
    }

    /**
     * 计算球形或圆柱选区的采样点。
     *
     * 球形画三个正交大圆；圆柱画上下两个水平圆与四条穿过上下底的母线。
     *
     * @param world 粒子所在世界。
     * @param finder 球形/圆柱查找器。
     * @param step 采样间距，单位格。
     * @return 轮廓采样点。
     */
    fun rangePoints(world: World, finder: RangeNearestEntityFinder, step: Double): List<Location> {
        val center = finder.origin
        val result = ArrayList<Location>()
        val halfHeight = finder.halfHeight
        if (halfHeight == null) {
            appendCircle(result, world, center.x, center.y, center.z, finder.radius, PLANE_HORIZONTAL, step)
            appendCircle(result, world, center.x, center.y, center.z, finder.radius, PLANE_VERTICAL_Z, step)
            appendCircle(result, world, center.x, center.y, center.z, finder.radius, PLANE_VERTICAL_X, step)
            return result
        }
        appendCircle(
            result,
            world,
            center.x,
            center.y - halfHeight,
            center.z,
            finder.radius,
            PLANE_HORIZONTAL,
            step,
        )
        appendCircle(
            result,
            world,
            center.x,
            center.y + halfHeight,
            center.z,
            finder.radius,
            PLANE_HORIZONTAL,
            step,
        )
        for (index in 0..3) {
            val radians = Math.toRadians(90.0 * index)
            val x = center.x + cos(radians) * finder.radius
            val z = center.z + sin(radians) * finder.radius
            appendLine(
                result,
                world,
                x,
                center.y - halfHeight,
                z,
                x,
                center.y + halfHeight,
                z,
                step,
            )
        }
        return result
    }

    /**
     * 向施法玩家发送单个粒子。
     *
     * @param player 施法玩家。
     * @param point 粒子坐标。
     */
    private fun spawn(player: Player, point: Location) {
        player.spawnParticle(particle, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0)
    }

    /**
     * 取调试查看者。
     *
     * @param sender 本次执行的施法者。
     * @return 施法者是玩家时返回该玩家，否则返回 null。
     */
    private fun viewerOf(sender: Any?): Player? {
        if (sender is Player) {
            return sender
        }
        return null
    }

    /**
     * 沿线段均匀取点写入结果集，包含起点、不包含终点，避免相邻棱在角点重复。
     *
     * @param target 结果集。
     * @param world 粒子所在世界。
     * @param fromX 起点 X。
     * @param fromY 起点 Y。
     * @param fromZ 起点 Z。
     * @param toX 终点 X。
     * @param toY 终点 Y。
     * @param toZ 终点 Z。
     * @param step 采样间距，单位格。
     */
    private fun appendLine(
        target: MutableList<Location>,
        world: World,
        fromX: Double,
        fromY: Double,
        fromZ: Double,
        toX: Double,
        toY: Double,
        toZ: Double,
        step: Double,
    ) {
        val deltaX = toX - fromX
        val deltaY = toY - fromY
        val deltaZ = toZ - fromZ
        val length = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
        val segments = ceil(length / step).toInt()
        val safeSegments = if (segments < 1) 1 else segments
        for (index in 0 until safeSegments) {
            val ratio = index.toDouble() / safeSegments
            target.add(Location(world, fromX + deltaX * ratio, fromY + deltaY * ratio, fromZ + deltaZ * ratio))
        }
    }

    /**
     * 沿扇形半径方向取点写入结果集，包含顶点。
     *
     * @param target 结果集。
     * @param world 粒子所在世界。
     * @param originX 扇形顶点 X。
     * @param planeY 绘制平面高度。
     * @param originZ 扇形顶点 Z。
     * @param yaw 半径方向朝向。
     * @param distance 半径长度。
     * @param step 采样间距，单位格。
     */
    private fun appendRadial(
        target: MutableList<Location>,
        world: World,
        originX: Double,
        planeY: Double,
        originZ: Double,
        yaw: Double,
        distance: Double,
        step: Double,
    ) {
        val segments = ceil(distance / step).toInt()
        val safeSegments = if (segments < 1) 1 else segments
        for (index in 0..safeSegments) {
            val current = distance * index / safeSegments
            target.add(radialPoint(world, originX, planeY, originZ, yaw, current))
        }
    }

    /**
     * 按朝向与距离取扇形平面上的点。
     *
     * @param world 粒子所在世界。
     * @param originX 扇形顶点 X。
     * @param planeY 绘制平面高度。
     * @param originZ 扇形顶点 Z。
     * @param yaw 朝向，与 Minecraft yaw 一致。
     * @param distance 距离顶点的距离。
     * @return 平面坐标。
     */
    private fun radialPoint(
        world: World,
        originX: Double,
        planeY: Double,
        originZ: Double,
        yaw: Double,
        distance: Double,
    ): Location {
        val radians = Math.toRadians(yaw)
        return Location(world, originX - sin(radians) * distance, planeY, originZ + cos(radians) * distance)
    }

    /**
     * 取圆周采样点写入结果集。
     *
     * @param target 结果集。
     * @param world 粒子所在世界。
     * @param centerX 圆心 X。
     * @param centerY 圆心 Y。
     * @param centerZ 圆心 Z。
     * @param radius 圆半径。
     * @param plane 圆所在平面，取本类的 PLANE_* 常量。
     * @param step 采样间距，单位格。
     */
    private fun appendCircle(
        target: MutableList<Location>,
        world: World,
        centerX: Double,
        centerY: Double,
        centerZ: Double,
        radius: Double,
        plane: Int,
        step: Double,
    ) {
        val circumference = 2.0 * Math.PI * radius
        val segments = ceil(circumference / step).toInt()
        val safeSegments = if (segments < 3) 3 else segments
        for (index in 0 until safeSegments) {
            val radians = 2.0 * Math.PI * index / safeSegments
            val first = cos(radians) * radius
            val second = sin(radians) * radius
            if (plane == PLANE_HORIZONTAL) {
                target.add(Location(world, centerX + first, centerY, centerZ + second))
            } else if (plane == PLANE_VERTICAL_Z) {
                target.add(Location(world, centerX + first, centerY + second, centerZ))
            } else {
                target.add(Location(world, centerX, centerY + first, centerZ + second))
            }
        }
    }
}
