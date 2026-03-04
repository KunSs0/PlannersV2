package com.gitee.planners.module.script.finder

import com.gitee.planners.api.common.util.SectorNearestEntityFinder
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.ProxyTargetContainer
import com.gitee.planners.util.math.createIdentityMatrix
import com.gitee.planners.util.math.rotate
import com.gitee.planners.util.math.transform
import com.gitee.planners.util.math.translate

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import taboolib.common.util.Vector
import taboolib.common5.cdouble
import taboolib.module.navigation.BoundingBox
import taboolib.module.navigation.NMS
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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
    private val extraTargets: MutableList<ProxyTarget<*>> = mutableListOf()
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

    // === Step 1: 简单过滤器 ===

    /** 添加施法者自身到结果集 */
    fun self(): TargetFinder {
        sender?.let { entities.add(it) }
        return this
    }

    /** 从结果集中排除施法者 */
    fun their(): TargetFinder {
        sender?.let { s -> entities.removeAll { it.uniqueId == s.uniqueId } }
        return this
    }

    /** 排除指定范围内的实体 */
    fun excludeRange(r: Double): TargetFinder {
        entities.removeAll { it.location.distance(origin) <= r }
        return this
    }

    /** 移除已死亡的实体 */
    fun remainLiving(): TargetFinder {
        entities.removeAll { it.isDead }
        return this
    }

    /** 合并其他目标到当前结果集 */
    fun merge(targets: Any): TargetFinder {
        when (targets) {
            is ProxyTargetContainer -> targets.forEach { t ->
                val entity = (t as? ProxyTarget.BukkitEntity)?.instance as? LivingEntity
                if (entity != null) entities.add(entity)
            }
            is ProxyTarget.BukkitEntity -> {
                val entity = targets.instance as? LivingEntity
                if (entity != null) entities.add(entity)
            }
            is LivingEntity -> entities.add(targets)
        }
        return this
    }

    /** 从当前结果集中移除指定目标 */
    fun unmerge(targets: Any): TargetFinder {
        when (targets) {
            is ProxyTargetContainer -> {
                val uuids = targets.filterIsInstance<ProxyTarget.BukkitEntity>()
                    .map { it.instance.uniqueId }.toSet()
                entities.removeAll { it.uniqueId in uuids }
            }
            is ProxyTarget.BukkitEntity -> {
                val uuid = targets.instance.uniqueId
                entities.removeAll { it.uniqueId == uuid }
            }
            is LivingEntity -> entities.removeAll { it.uniqueId == targets.uniqueId }
        }
        return this
    }

    // === Step 2: 形状选择器 ===

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

    /**
     * OBB 矩形体选择器
     * @param width 宽度
     * @param height 高度
     * @param depth 深度
     * @param offsetX X 偏移
     * @param offsetY Y 偏移
     * @param offsetZ Z 偏移
     */
    fun rectangle(
        width: Double,
        height: Double,
        depth: Double,
        offsetX: Double = 0.0,
        offsetY: Double = 0.0,
        offsetZ: Double = 0.0
    ): TargetFinder {
        origin.world ?: return this
        val block = OBBBlock(width, height, depth, Vector(offsetX, offsetY, offsetZ))
        val found = block.find(origin)
            .filterIsInstance<LivingEntity>()
            .filter { includeSelf || sender == null || it.uniqueId != sender!!.uniqueId }
        entities.addAll(found)
        return this
    }

    // === Step 3: 转换器 ===

    /**
     * 获取视线方向的目标方块并加入结果
     * @param distance 最大检测距离
     */
    fun lookBlock(distance: Int = 5): TargetFinder {
        val entity = sender ?: return this
        val block = entity.getTargetBlock(null, distance)
        extraTargets.add(ProxyTarget.of(block))
        return this
    }

    /**
     * 将结果集中的实体转换为位置目标
     * @param rule "eye" 取眼睛位置，否则取脚下位置
     */
    fun toLoc(rule: String = "default"): TargetFinder {
        entities.forEach { entity ->
            val loc = when (rule.lowercase()) {
                "eye" -> (entity as? LivingEntity)?.eyeLocation ?: entity.location
                else -> entity.location
            }
            extraTargets.add(ProxyTarget.of(loc))
        }
        entities.clear()
        return this
    }

    // === 构建结果 ===

    fun build(): ProxyTargetContainer {
        return ProxyTargetContainer().apply {
            entities.forEach { add(ProxyTarget.of(it)) }
            extraTargets.forEach { add(it) }
        }
    }

    // === OBB 矩形体内部实现 ===

    private class OBBBlock(
        val width: Double,
        val height: Double,
        val depth: Double,
        val offset: Vector
    ) {
        enum class VertexType {
            BOTTOM_LEFT_BACK {
                override fun build(o: Vector, s: Vector) = Vector(o.x, o.y, o.z)
            },
            BOTTOM_RIGHT_BACK {
                override fun build(o: Vector, s: Vector) = Vector(o.x, o.y, o.z + s.z)
            },
            BOTTOM_LEFT_FRONT {
                override fun build(o: Vector, s: Vector) = Vector(o.x + s.x, o.y, o.z)
            },
            BOTTOM_RIGHT_FRONT {
                override fun build(o: Vector, s: Vector) = Vector(o.x + s.x, o.y, o.z + s.z)
            },
            TOP_LEFT_BACK {
                override fun build(o: Vector, s: Vector) = Vector(o.x, o.y + s.y, o.z)
            },
            TOP_RIGHT_BACK {
                override fun build(o: Vector, s: Vector) = Vector(o.x, o.y + s.y, o.z + s.z)
            },
            TOP_LEFT_FRONT {
                override fun build(o: Vector, s: Vector) = Vector(o.x + s.x, o.y + s.y, o.z)
            },
            TOP_RIGHT_FRONT {
                override fun build(o: Vector, s: Vector) = Vector(o.x + s.x, o.y + s.y, o.z + s.z)
            };

            abstract fun build(o: Vector, s: Vector): Vector
        }

        fun buildVertices(location: Vector, yaw: Float): Map<VertexType, Vector> {
            val matrix = createIdentityMatrix()
                .translate(location.x, location.y, location.z)
            val o = Vector(-width / 2, 0.0, -depth / 2)
            val s = Vector(width, height, depth)
            return VertexType.values().associateWith { vt ->
                val v = vt.build(o, s)
                matrix
                    .rotate(-Math.toRadians(yaw.cdouble), 1)
                    .translate(offset.x, offset.y, offset.z)
                    .transform(v.x, v.y, v.z, 1.0)
            }
        }

        fun buildVertex(location: Vector, yaw: Float, type: VertexType): Vector {
            val matrix = createIdentityMatrix()
                .translate(location.x, location.y, location.z)
            val o = Vector(-width / 2, 0.0, -depth / 2)
            val s = Vector(width, height, depth)
            val v = type.build(o, s)
            return matrix
                .rotate(-Math.toRadians(yaw.cdouble), 1)
                .translate(offset.x, offset.y, offset.z)
                .transform(v.x, v.y, v.z, 1.0)
        }

        fun find(location: Location): List<Entity> {
            val loc = Vector(location.x, location.y, location.z)
            val vertices = buildVertices(loc, location.yaw)
            val values = vertices.values
            val halfX = (values.maxOf { it.x } - values.minOf { it.x })
            val halfY = (values.maxOf { it.y } - values.minOf { it.y })
            val halfZ = (values.maxOf { it.z } - values.minOf { it.z })
            val nearby = location.world!!.getNearbyEntities(location, halfX, halfY, halfZ)
            return nearby.filter { entity ->
                val aabb = NMS.instance.getBoundingBox(entity) ?: return@filter false
                obbIntersectsAABB(loc, aabb, location.yaw)
            }
        }

        private fun obbIntersectsAABB(location: Vector, aabb: BoundingBox, yaw: Float): Boolean {
            val rad = Math.toRadians(yaw.cdouble)
            val cosY = cos(rad)
            val sinY = sin(rad)

            // OBB 主轴
            val axisX = Vector(cosY, 0.0, -sinY)
            val axisY = Vector(0.0, 1.0, 0.0)
            val axisZ = Vector(sinY, 0.0, cosY)

            // AABB 主轴
            val nX = Vector(1.0, 0.0, 0.0)
            val nY = Vector(0.0, 1.0, 0.0)
            val nZ = Vector(0.0, 0.0, 1.0)

            val axes = arrayOf(
                axisX, axisY, axisZ, nX, nY, nZ,
                axisX.getCrossProduct(nX), axisX.getCrossProduct(nY), axisX.getCrossProduct(nZ),
                axisY.getCrossProduct(nX), axisY.getCrossProduct(nY), axisY.getCrossProduct(nZ),
                axisZ.getCrossProduct(nX), axisZ.getCrossProduct(nY), axisZ.getCrossProduct(nZ)
            )

            return axes.filter { it.length() > 1e-6 }.all { axis ->
                val n = axis.normalize()
                val (minA, maxA) = projectAABB(aabb, n)
                val (minB, maxB) = projectOBB(location, yaw, n)
                !(maxA < minB || maxB < minA)
            }
        }

        private fun projectAABB(aabb: BoundingBox, axis: Vector): Pair<Double, Double> {
            val vertices = arrayOf(
                Vector(aabb.minX, aabb.minY, aabb.minZ),
                Vector(aabb.maxX, aabb.minY, aabb.minZ),
                Vector(aabb.minX, aabb.maxY, aabb.minZ),
                Vector(aabb.minX, aabb.minY, aabb.maxZ),
                Vector(aabb.maxX, aabb.maxY, aabb.minZ),
                Vector(aabb.minX, aabb.maxY, aabb.maxZ),
                Vector(aabb.maxX, aabb.minY, aabb.maxZ),
                Vector(aabb.maxX, aabb.maxY, aabb.maxZ)
            )
            val projections = vertices.map { it.dot(axis) }
            return projections.min() to projections.max()
        }

        private fun projectOBB(location: Vector, yaw: Float, axis: Vector): Pair<Double, Double> {
            val projections = VertexType.values().map { buildVertex(location, yaw, it).dot(axis) }
            return projections.min() to projections.max()
        }
    }
}
