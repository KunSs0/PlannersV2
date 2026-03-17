package com.gitee.planners.module.script.finder

import com.gitee.planners.api.common.util.SectorNearestEntityFinder
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.ProxyTargetContainer

import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity

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
class TargetFinder(
    private var origin: Location,
    private var sender: LivingEntity? = null
) {
    private val entities: MutableSet<LivingEntity> = mutableSetOf()
    private var includeSelf: Boolean = false

    enum class SortType { NAME, DISTANCE, RANDOM }

    // === 选择器 (立即执行，累加结果) ===

    fun range(r: Double): TargetFinder {
        val world = origin.world ?: return this
        val nearby = world.getNearbyEntities(origin, r, r, r)
            .filterIsInstance<LivingEntity>()
            .filter { it.location.distance(origin) <= r }
            .filter { includeSelf || sender == null || it.uniqueId != sender!!.uniqueId }
        entities.addAll(nearby)
        return this
    }

    // === 状态修改 ===

    fun origin(location: Location): TargetFinder {
        this.origin = location
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
    fun sector(radius: Double, angle: Double, yaw: Float? = null): TargetFinder {
        val world = origin.world ?: return this
        val loc = origin.clone()
        if (yaw != null) loc.yaw = yaw
        val sampling = world.getNearbyEntities(loc, radius, radius, radius)
            .filter { it is LivingEntity && (includeSelf || sender == null || it.uniqueId != sender!!.uniqueId) }
        val found = SectorNearestEntityFinder(loc, angle, radius, loc.yaw, sampling).request()
            .filterIsInstance<LivingEntity>()
        entities.addAll(found)
        return this
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
        return ProxyTargetContainer().apply {
            entities.forEach { add(ProxyTarget.of(it)) }
        }
    }
}
