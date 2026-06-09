package com.gitee.planners.module.script.finder

import com.gitee.planners.Planners
import com.gitee.planners.api.common.facing.EntityFacingProviders
import com.gitee.planners.api.common.util.SectorNearestEntityFinder
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.ProxyTargetContainer

import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import taboolib.common.util.runSync
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
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
