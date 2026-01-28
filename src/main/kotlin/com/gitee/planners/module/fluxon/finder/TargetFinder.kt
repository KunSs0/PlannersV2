package com.gitee.planners.module.fluxon.finder

import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.ProxyTargetContainer
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.registerFunction
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.tabooproject.fluxon.runtime.java.Export
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 链式目标查找器 - 立即执行模式
 *
 * 示例：
 * ```fluxon
 * // 基础用法
 * var targets = finder()::range(10)::type("zombie")::limit(3)::build()
 *
 * // 多区域选择
 * var multi = finder()::range(10)::origin(locB)::range(5)::build()
 *
 * // 多类型 (OR 逻辑)
 * var undead = finder()::range(15)::type("zombie,skeleton")::build()
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

    @Export
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

    @Export
    fun origin(location: Location): TargetFinder {
        this.origin = location
        return this
    }

    @Export
    fun includeSelf(): TargetFinder {
        this.includeSelf = true
        return this
    }

    // === 过滤器 (立即执行，修改结果集) ===

    @Export
    fun type(type: String): TargetFinder {
        val types = type.split(",").map { it.trim() }.mapNotNull { name ->
            EntityType.values().find { it.name.equals(name, ignoreCase = true) }
        }
        if (types.isEmpty()) error("未知实体类型: $type")
        entities.retainAll { it.type in types }
        return this
    }

    @Export
    fun excludeType(type: String): TargetFinder {
        val types = type.split(",").map { it.trim() }.mapNotNull { name ->
            EntityType.values().find { it.name.equals(name, ignoreCase = true) }
        }
        if (types.isEmpty()) error("未知实体类型: $type")
        entities.removeAll { it.type in types }
        return this
    }

    @Export
    fun name(pattern: String): TargetFinder {
        val patterns = pattern.split(",").map { Regex(it.trim(), RegexOption.IGNORE_CASE) }
        entities.retainAll { entity -> patterns.any { it.containsMatchIn(entity.name) } }
        return this
    }

    @Export
    fun inWorld(world: String): TargetFinder {
        val worlds = world.split(",").map { it.trim().lowercase() }
        entities.retainAll { it.world.name.lowercase() in worlds }
        return this
    }

    // === 限制器 (立即执行) ===

    @Export
    fun limit(n: Int): TargetFinder {
        if (entities.size > n) {
            val toKeep = entities.take(n).toSet()
            entities.retainAll(toKeep)
        }
        return this
    }

    @Export
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

    @Export
    fun sortReverse(): TargetFinder {
        val reversed = entities.reversed()
        entities.clear()
        entities.addAll(reversed)
        return this
    }

    @Export
    fun shuffle(): TargetFinder {
        val shuffled = entities.shuffled()
        entities.clear()
        entities.addAll(shuffled)
        return this
    }

    // === 构建结果 ===

    @Export
    fun build(): ProxyTargetContainer {
        return ProxyTargetContainer().apply {
            entities.forEach { add(ProxyTarget.of(it)) }
        }
    }

    companion object {

        @Awake(LifeCycle.LOAD)
        private fun init() {
            val runtime = FluxonScriptCache.runtime

            runtime.exportRegistry.registerClass(TargetFinder::class.java)

            // finder([origin]) -> TargetFinder
            runtime.registerFunction("finder", listOf(0, 1)) { ctx ->
                val origin = resolveLocation(ctx.getRef(0))
                    ?: (ctx.environment.rootVariables["sender"] as? LivingEntity)?.location
                    ?: error("无法解析 origin，请传入 Location 或确保 sender 是 LivingEntity")

                val sender = ctx.environment.rootVariables["sender"] as? LivingEntity
                TargetFinder(origin, sender)
            }
        }

        private fun resolveLocation(arg: Any?): Location? {
            return when (arg) {
                is Location -> arg
                is ProxyTarget.BukkitLocation -> arg.getBukkitLocation()
                is ProxyTarget.BukkitEntity -> arg.getBukkitLocation()
                is LivingEntity -> arg.location
                else -> null
            }
        }
    }
}
