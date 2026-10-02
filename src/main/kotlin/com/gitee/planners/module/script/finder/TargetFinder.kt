package com.gitee.planners.module.script.finder

import com.gitee.planners.Planners
import com.gitee.planners.api.common.facing.EntityFacingProviders
import com.gitee.planners.api.common.util.RectNearestEntityFinder
import com.gitee.planners.api.common.util.SectorNearestEntityFinder
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.ProxyTargetContainer

import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import taboolib.common.util.runSync
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * 链式目标查找器 - 立即执行模式
 *
 * 示例：
 * ```nova
 * // 基础用法
 * var targets = finder().range(10).type("zombie").limit(3).build()
 *
 * // 多区域选择
 * var multi = finder().range(10).origin(locB).range(5).build()
 *
 * // 多类型 (OR 逻辑)
 * var undead = finder().range(15).type("zombie,skeleton").build()
 * ```
 */
class TargetFinder @JvmOverloads constructor(
    private var origin: Location,
    private var sender: LivingEntity? = null,
    private var facingYaw: Float? = null
) {
    private val entities: MutableSet<LivingEntity> = mutableSetOf()
    private var includeSelf: Boolean = false

    enum class SortType { NAME, DISTANCE, RANDOM }

    init {
        if (sender != null && facingYaw == null) {
            setFacing()
        }
    }

    // === 选择器 (立即执行，累加结果) ===

    fun range(r: Double): TargetFinder {
        val nearby = runSync {
            val world = origin.world
            if (world == null) {
                return@runSync emptyList()
            }
            world.getNearbyEntities(origin, r, r, r)
                .filterIsInstance<LivingEntity>()
                .filter { it.location.distance(origin) <= r }
                .filter { includeSelf || sender == null || it.uniqueId != sender!!.uniqueId }
        }
        entities.addAll(nearby)
        return this
    }

    // === 状态修改 ===

    fun origin(location: Location): TargetFinder {
        this.origin = location
        this.facingYaw = location.yaw
        return this
    }

    fun origin(entity: LivingEntity): TargetFinder {
        this.origin = entity.location
        this.facingYaw = resolveFacingYaw(entity)
        return this
    }

    /**
     * 从当前施法者同步目标选择朝向。
     *
     * @return 当前目标查找器。
     * @throws IllegalStateException 当前查找器未绑定施法者时抛出。
     */
    fun setFacing(): TargetFinder {
        val currentSender = sender
        if (currentSender == null) {
            throw IllegalStateException("TargetFinder.setFacing() requires a sender")
        }
        facingYaw = resolveFacingYaw(currentSender)
        return this
    }

    /**
     * 覆盖目标选择朝向。
     *
     * @param yaw 覆盖后的世界朝向。
     * @return 当前目标查找器。
     */
    fun setFacing(yaw: Float): TargetFinder {
        facingYaw = yaw
        return this
    }

    fun includeSelf(): TargetFinder {
        this.includeSelf = true
        return this
    }

    /**
     * 通过全局朝向 Provider 解析实体当前朝向。
     *
     * @param entity 需要解析朝向的实体。
     * @return Provider 返回的世界朝向。
     */
    private fun resolveFacingYaw(entity: LivingEntity): Float {
        return EntityFacingProviders.getFacingYaw(entity)
    }

    /**
     * 扇形选择器
     * @param radius 半径
     * @param angle 扇形角度（度）
     * @param yaw 可选方向覆盖，默认使用 origin 的 yaw
     */
    @JvmOverloads
    fun sector(radius: Double, angle: Double, yaw: Float? = null): TargetFinder {
        return sector(radius, angle, yaw, 0.0, 0.0, 0.0)
    }

    /**
     * 以当前原点为基准，按朝向偏移后创建扇形选择区域。
     */
    fun sector(
        radius: Double,
        angle: Double,
        yaw: Float? = null,
        offsetX: Double,
        offsetY: Double,
        offsetZ: Double
    ): TargetFinder {
        val found = runSync {
            val world = origin.world
            if (world == null) {
                return@runSync emptyList()
            }
            val loc = origin.clone()
            val directionYaw = yaw ?: facingYaw ?: loc.yaw
            val radians = Math.toRadians(directionYaw.toDouble())
            val forwardX = -kotlin.math.sin(radians)
            val forwardZ = kotlin.math.cos(radians)
            val rightX = kotlin.math.cos(radians)
            val rightZ = kotlin.math.sin(radians)
            loc.add(
                offsetZ * forwardX + offsetX * rightX,
                offsetY,
                offsetZ * forwardZ + offsetX * rightZ
            )
            val preRadius = radius + kotlin.math.hypot(offsetX, offsetZ)
            val sampling = world.getNearbyEntities(loc, preRadius, preRadius, preRadius)
                .filter { it is LivingEntity && (includeSelf || sender == null || it.uniqueId != sender!!.uniqueId) }
            val result = SectorNearestEntityFinder(loc, angle, radius, directionYaw, sampling).request()
                .filterIsInstance<LivingEntity>()
            if (Planners.sectorSelectorDebug) {
                spawnSectorDebugParticles(loc, radius, angle, directionYaw)
            }
            result
        }
        entities.addAll(found)
        return this
    }

    /**
     * 从当前原点创建无偏移、沿 facing 方向的矩形（3D Box）选择区域。
     *
     * @param width 矩形宽度（左右方向，完整宽度）。
     * @param height 矩形高度（上下方向，完整高度）。
     * @param length 矩形长度（前后方向，完整长度）。
     * @return 当前目标查找器。
     */
    fun rect(width: Double, height: Double, length: Double): TargetFinder {
        return rect(width, height, length, 0.0, 0.0, 0.0)
    }

    /**
     * 从当前原点创建带偏移、沿 facing 方向的矩形（3D Box）选择区域。
     *
     * ```nova
     * finder().rect(5, 3, 4).build()
     * finder().rect(5, 3, 4, 0, 0, 2).build()
     * ```
     *
     * @param width 矩形宽度（左右方向，完整宽度）。
     * @param height 矩形高度（上下方向，完整高度）。
     * @param length 矩形长度（前后方向，完整长度）。
     * @param offsetX 左右方向偏移量。
     * @param offsetY 上下方向偏移量。
     * @param offsetZ 前后方向偏移量。
     * @return 当前目标查找器。
     */
    fun rect(
        width: Double,
        height: Double,
        length: Double,
        offsetX: Double,
        offsetY: Double,
        offsetZ: Double
    ): TargetFinder {
        val found = runSync {
            val world = origin.world
            if (world == null) {
                return@runSync emptyList()
            }
            val loc = origin.clone()
            val directionYaw = facingYaw ?: loc.yaw

            // 预筛选半径：覆盖 rect 最远角点到 origin 的距离
            val preRadius = hypot(
                hypot(width / 2.0 + abs(offsetX), length / 2.0 + abs(offsetZ)),
                height / 2.0 + abs(offsetY)
            )

            val sampling = world.getNearbyEntities(loc, preRadius, preRadius, preRadius)
                .filter { it is LivingEntity && (includeSelf || sender == null || it.uniqueId != sender!!.uniqueId) }
            val result = RectNearestEntityFinder(
                loc,
                width,
                height,
                length,
                directionYaw,
                offsetX,
                offsetY,
                offsetZ,
                sampling
            )
                .request()
                .filterIsInstance<LivingEntity>()

            if (Planners.sectorSelectorDebug) {
                spawnRectDebugParticles(
                    loc,
                    width,
                    height,
                    length,
                    directionYaw,
                    offsetX,
                    offsetY,
                    offsetZ
                )
            }
            result
        }
        entities.addAll(found)
        return this
    }

    private fun spawnSectorDebugParticles(origin: Location, radius: Double, angle: Double, directionYaw: Float) {
        val world = origin.world
        if (world == null) {
            return
        }
        val safeRadius = radius.coerceAtLeast(0.0)
        if (safeRadius == 0.0) {
            return
        }
        val particle = Planners.sectorSelectorDebugParticle.get()
        val step = Planners.sectorSelectorDebugStep.coerceAtLeast(0.1)
        val y = origin.y + Planners.sectorSelectorDebugYOffset
        val halfAngle = angle.coerceIn(0.0, 360.0) / 2.0

        for (edgeYaw in listOf(directionYaw - halfAngle, directionYaw + halfAngle)) {
            val edgeSteps = ceil(safeRadius / step).toInt().coerceAtLeast(1)
            for (i in 0..edgeSteps) {
                val distance = minOf(i * step, safeRadius)
                spawnParticle(world, particle, origin.x, y, origin.z, edgeYaw, distance)
            }
        }

        val arcStepAngle = max(1.0, Math.toDegrees(step / safeRadius))
        val arcSteps = ceil((halfAngle * 2.0) / arcStepAngle).toInt().coerceAtLeast(1)
        for (i in 0..arcSteps) {
            val offset = -halfAngle + (halfAngle * 2.0) * i / arcSteps
            spawnParticle(world, particle, origin.x, y, origin.z, directionYaw + offset, safeRadius)
        }
    }

    private fun spawnParticle(
        world: org.bukkit.World,
        particle: Particle,
        originX: Double,
        originY: Double,
        originZ: Double,
        yaw: Double,
        distance: Double
    ) {
        val radians = yaw / 180.0 * PI
        val x = originX - sin(radians) * distance
        val z = originZ + cos(radians) * distance
        world.spawnParticle(particle, x, originY, z, 1, 0.0, 0.0, 0.0, 0.0)
    }

    /**
     * 绘制矩形选择器的 debug 粒子（顶面 + 底面轮廓）
     */
    private fun spawnRectDebugParticles(
        origin: Location, w: Double, h: Double, z: Double,
        directionYaw: Float, ox: Double, oy: Double, oz: Double
    ) {
        val world = origin.world
        if (world == null) {
            return
        }
        val particle = Planners.sectorSelectorDebugParticle.get()
        val step = Planners.sectorSelectorDebugStep.coerceAtLeast(0.5)

        val radians = Math.toRadians(directionYaw.toDouble())
        val fx = -sin(radians)   // forward x
        val fz = cos(radians)    // forward z
        val rx = cos(radians)    // right x
        val rz = sin(radians)    // right z

        val cx = origin.x + oz * fx + ox * rx
        val cy = origin.y + oy
        val cz = origin.z + oz * fz + ox * rz
        val hw = w / 2.0
        val hh = h / 2.0
        val hl = z / 2.0

        fun corner(lx: Double, lz: Double): Pair<Double, Double> {
            return Pair(cx + lx * rx + lz * fx, cz + lx * rz + lz * fz)
        }

        fun line(x1: Double, z1: Double, x2: Double, z2: Double, lineY: Double) {
            val dist = hypot(x2 - x1, z2 - z1)
            val steps = ceil(dist / step).toInt().coerceAtLeast(1)
            for (i in 0..steps) {
                val t = i.toDouble() / steps
                val px = x1 + (x2 - x1) * t
                val pz = z1 + (z2 - z1) * t
                world.spawnParticle(particle, px, lineY, pz, 1, 0.0, 0.0, 0.0, 0.0)
            }
        }

        val corners = listOf(
            corner(-hw, -hl), corner(hw, -hl),
            corner(hw, hl), corner(-hw, hl)
        )

        val topY = cy + hh + Planners.sectorSelectorDebugYOffset
        val botY = cy - hh + Planners.sectorSelectorDebugYOffset

        for (drawY in listOf(topY, botY)) {
            for (i in 0 until 4) {
                val (x1, z1) = corners[i]
                val (x2, z2) = corners[(i + 1) % 4]
                line(x1, z1, x2, z2, drawY)
            }
        }
    }

    // === 过滤器 (立即执行，修改结果集) ===

    fun type(type: String): TargetFinder {
        val types = type.split(",").map { it.trim() }.mapNotNull { name ->
            EntityType.values().find { it.name.equals(name, ignoreCase = true) }
        }
        if (types.isEmpty()) {
            error("Unknown entity type: $type")
        }
        entities.retainAll { it.type in types }
        return this
    }

    fun excludeType(type: String): TargetFinder {
        val types = type.split(",").map { it.trim() }.mapNotNull { name ->
            EntityType.values().find { it.name.equals(name, ignoreCase = true) }
        }
        if (types.isEmpty()) {
            error("Unknown entity type: $type")
        }
        entities.removeAll { it.type in types }
        return this
    }

    fun name(pattern: String): TargetFinder {
        val patterns = pattern.split(",").map { Regex(it.trim(), RegexOption.IGNORE_CASE) }
        entities.retainAll { entity -> patterns.any { it.containsMatchIn(entity.name) } }
        return this
    }

    fun inWorld(world: String): TargetFinder {
        val worlds = world.split(",").map { it.trim().lowercase() }
        entities.retainAll { it.world.name.lowercase() in worlds }
        return this
    }

    // === 限制器 (立即执行) ===

    fun limit(n: Int): TargetFinder {
        if (entities.size > n) {
            val toKeep = entities.take(n).toSet()
            entities.retainAll(toKeep)
        }
        return this
    }

    fun sort(type: String): TargetFinder {
        val sortType = SortType.values().find { it.name.equals(type, ignoreCase = true) }
        if (sortType == null) {
            error("Unknown target sort type: $type")
        }
        val sorted = when (sortType) {
            SortType.NAME -> entities.sortedBy { it.name }
            SortType.DISTANCE -> entities.sortedBy { it.location.distance(origin) }
            SortType.RANDOM -> entities.shuffled()
        }
        entities.clear()
        entities.addAll(sorted)
        return this
    }

    fun sortReverse(): TargetFinder {
        val reversed = entities.reversed()
        entities.clear()
        entities.addAll(reversed)
        return this
    }

    fun shuffle(): TargetFinder {
        val shuffled = entities.shuffled()
        entities.clear()
        entities.addAll(shuffled)
        return this
    }

    // === 构建结果 ===

    fun build(): ProxyTargetContainer {
        val container = ProxyTargetContainer()
        for (entity in entities) {
            container.add(ProxyTarget.of(entity))
        }
        return container
    }
}
