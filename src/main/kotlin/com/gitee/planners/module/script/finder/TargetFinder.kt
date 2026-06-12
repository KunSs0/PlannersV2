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
 * ```js
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

    // === 选择器 (立即执行，累加结果) ===

    fun range(r: Double): TargetFinder {
        val nearby = runSync {
            val world = origin.world ?: return@runSync emptyList()
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
        this.facingYaw = EntityFacingProviders.getFacingYaw(entity)
        return this
    }

    fun includeSelf(): TargetFinder {
        this.includeSelf = true
        return this
    }

    /**
     * 扇形选择器
     * @param radius 半径
     * @param angle 扇形角度（度）
     * @param yaw 可选方向覆盖，默认使用 origin 的 yaw
     */
    @JvmOverloads
    fun sector(radius: Double, angle: Double, yaw: Float? = null): TargetFinder {
        val found = runSync {
            val world = origin.world ?: return@runSync emptyList()
            val loc = origin.clone()
            val directionYaw = yaw ?: facingYaw ?: loc.yaw
            val sampling = world.getNearbyEntities(loc, radius, radius, radius)
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
     * 矩形（3D Box）选择器
     * 以 origin + offset 为中心、沿 facing 方向的矩形包围盒。
     *
     * @param w      矩形宽度（左右方向，full width）
     * @param h      矩形高度（上下方向，full height）
     * @param z      矩形长度（前后方向，full length）
     * @param offset 可选偏移量 {x: 左右, y: 上下, z: 前后}，默认均为 0
     *
     * ```js
     * finder().rect(5, 3, 4).build()
     * finder().rect(5, 3, 4, {x: 0, y: 0, z: 2}).build()
     * ```
     */
    fun rect(w: Double, h: Double, z: Double, offset: Map<String, Any?>? = null): TargetFinder {
        val ox = (offset?.get("x") as? Number)?.toDouble() ?: 0.0
        val oy = (offset?.get("y") as? Number)?.toDouble() ?: 0.0
        val oz = (offset?.get("z") as? Number)?.toDouble() ?: 0.0

        val found = runSync {
            val world = origin.world ?: return@runSync emptyList()
            val loc = origin.clone()
            val directionYaw = facingYaw ?: loc.yaw

            // 预筛选半径：覆盖 rect 最远角点到 origin 的距离
            val preRadius = hypot(
                hypot(w / 2.0 + abs(ox), z / 2.0 + abs(oz)),
                h / 2.0 + abs(oy)
            )

            val sampling = world.getNearbyEntities(loc, preRadius, preRadius, preRadius)
                .filter { it is LivingEntity && (includeSelf || sender == null || it.uniqueId != sender!!.uniqueId) }
            val result = RectNearestEntityFinder(loc, w, h, z, directionYaw, ox, oy, oz, sampling)
                .request()
                .filterIsInstance<LivingEntity>()

            if (Planners.sectorSelectorDebug) {
                spawnRectDebugParticles(loc, w, h, z, directionYaw, ox, oy, oz)
            }
            result
        }
        entities.addAll(found)
        return this
    }

    private fun spawnSectorDebugParticles(origin: Location, radius: Double, angle: Double, directionYaw: Float) {
        val world = origin.world ?: return
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
        val world = origin.world ?: return
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
        if (types.isEmpty()) error("未知实体类型: $type")
        entities.retainAll { it.type in types }
        return this
    }

    fun excludeType(type: String): TargetFinder {
        val types = type.split(",").map { it.trim() }.mapNotNull { name ->
            EntityType.values().find { it.name.equals(name, ignoreCase = true) }
        }
        if (types.isEmpty()) error("未知实体类型: $type")
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
            ?: error("未知排序类型: $type")
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
