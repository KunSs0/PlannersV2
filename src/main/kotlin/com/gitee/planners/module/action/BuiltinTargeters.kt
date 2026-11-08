package com.gitee.planners.module.action

import com.gitee.planners.api.common.facing.EntityFacingProviders
import com.gitee.planners.api.common.util.RectNearestEntityFinder
import com.gitee.planners.api.common.util.SectorNearestEntityFinder
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * 内置目标器实现。
 *
 * 严格按书写顺序执行：源目标器产出容器，过滤器在容器内筛选，排序调整顺序，
 * 截断保留前 N 个。源目标器支持 `mode=replace|add`，默认替换。
 */
object BuiltinTargeters {

    /** 源目标器的容器合并方式。 */
    private const val MODE_REPLACE = "replace"

    /** 源目标器的容器合并方式。 */
    private const val MODE_ADD = "add"

    /** 准星射线检测使用的实体膨胀半径，单位格。 */
    private const val CROSSHAIR_RAY_SIZE = 0.35

    /** 以施法者自身为容器。 */
    object Self : ActionTargeter {

        override val id = "self"

        override val aliases = listOf("self")

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val target = toTarget(scope.sender)
            if (target == null) {
                return emptyList()
            }
            return listOf(target)
        }
    }

    /**
     * 以本次技能的当前目标为容器。
     *
     * 单体技能不需要再写选区，直接用这一项；没有目标时容器为空，动作按空容器跳过。
     */
    object Current : ActionTargeter {

        override val id = "target"

        override val aliases = listOf("target")

        override val scope = ActionScope.SKILL

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val target = toTarget(scope.target)
            if (target == null) {
                return emptyList()
            }
            if (!target.isActive()) {
                return emptyList()
            }
            return listOf(target)
        }
    }

    /** 以施法者为中心按球形半径选取实体。 */
    object Range : ActionTargeter {

        override val id = "range"

        override val aliases = listOf("range")

        override val scope = ActionScope.SKILL

        override fun validate(arguments: ActionArguments) {
            arguments.require("radius")
            arguments.rejectUnknown("radius", "mode")
        }

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val source = scope.sender
            if (source !is LivingEntity) {
                return emptyList()
            }
            val radius = arguments.requireNumber(scope, "radius")
            if (radius <= 0.0) {
                return emptyList()
            }
            val origin = source.location
            val world = origin.world
            if (world == null) {
                return emptyList()
            }
            val found = ArrayList<ActionTarget>()
            for (entity in world.getNearbyEntities(origin, radius, radius, radius)) {
                if (entity !is LivingEntity) {
                    continue
                }
                if (entity.uniqueId == source.uniqueId) {
                    continue
                }
                if (entity.location.distance(origin) > radius) {
                    continue
                }
                found.add(ActionTarget.EntityTarget(entity))
            }
            return merge(scope, arguments, input, found)
        }
    }

    /** 以施法者朝向为中心的扇形选区。 */
    object Sector : ActionTargeter {

        override val id = "sector"

        override val aliases = listOf("sector")

        override val scope = ActionScope.SKILL

        override fun validate(arguments: ActionArguments) {
            arguments.require("radius")
            arguments.require("angle")
            arguments.rejectUnknown(
                "radius", "angle", "yaw", "offsetX", "offsetY", "offsetZ", "mode",
            )
        }

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val source = scope.sender
            if (source !is LivingEntity) {
                return emptyList()
            }
            val radius = arguments.requireNumber(scope, "radius")
            val angle = arguments.requireNumber(scope, "angle")
            if (radius <= 0.0 || angle <= 0.0) {
                return emptyList()
            }
            val origin = source.location
            val world = origin.world
            if (world == null) {
                return emptyList()
            }
            val yawOverride = arguments.number(scope, "yaw")
            val offsetX = arguments.number(scope, "offsetX") ?: 0.0
            val offsetY = arguments.number(scope, "offsetY") ?: 0.0
            val offsetZ = arguments.number(scope, "offsetZ") ?: 0.0
            val directionYaw = resolveYaw(source, yawOverride, origin)
            val center = offsetCenter(origin, directionYaw, offsetX, offsetY, offsetZ)
            val preRadius = radius + hypot(offsetX, offsetZ)
            val sampling = collectLiving(world.getNearbyEntities(center, preRadius, preRadius, preRadius), source)
            val found = ArrayList<ActionTarget>()
            val matched = SectorNearestEntityFinder(center, angle, radius, directionYaw, sampling).request()
            for (entity in matched) {
                if (entity is LivingEntity) {
                    found.add(ActionTarget.EntityTarget(entity))
                }
            }
            return merge(scope, arguments, input, found)
        }
    }

    /** 以施法者朝向为前的矩形选区。 */
    object Rect : ActionTargeter {

        override val id = "rect"

        override val aliases = listOf("rect")

        override val scope = ActionScope.SKILL

        override fun validate(arguments: ActionArguments) {
            arguments.require("width")
            arguments.require("height")
            arguments.require("length")
            arguments.rejectUnknown("width", "height", "length", "offsetX", "offsetY", "offsetZ", "mode")
        }

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val source = scope.sender
            if (source !is LivingEntity) {
                return emptyList()
            }
            val width = arguments.requireNumber(scope, "width")
            val height = arguments.requireNumber(scope, "height")
            val length = arguments.requireNumber(scope, "length")
            if (width <= 0.0 || height <= 0.0 || length <= 0.0) {
                return emptyList()
            }
            val origin = source.location
            val world = origin.world
            if (world == null) {
                return emptyList()
            }
            val offsetX = arguments.number(scope, "offsetX") ?: 0.0
            val offsetY = arguments.number(scope, "offsetY") ?: 0.0
            val offsetZ = arguments.number(scope, "offsetZ") ?: 0.0
            val directionYaw = resolveYaw(source, null, origin)
            val preRadius = hypot(hypot(width / 2.0 + abs(offsetX), length / 2.0 + abs(offsetZ)), height / 2.0 + abs(offsetY))
            val sampling = collectLiving(world.getNearbyEntities(origin, preRadius, preRadius, preRadius), source)
            val matched = RectNearestEntityFinder(
                origin,
                width,
                height,
                length,
                directionYaw,
                offsetX,
                offsetY,
                offsetZ,
                sampling,
            ).request()
            val found = ArrayList<ActionTarget>()
            for (entity in matched) {
                if (entity is LivingEntity) {
                    found.add(ActionTarget.EntityTarget(entity))
                }
            }
            return merge(scope, arguments, input, found)
        }
    }

    /** 以施法者位置为原点，可覆盖朝向。 */
    object Source : ActionTargeter {

        override val id = "source"

        override val aliases = listOf("source")

        override val scope = ActionScope.SKILL

        override fun validate(arguments: ActionArguments) {
            arguments.rejectUnknown("yaw")
        }

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val origin = originOf(scope)
            if (origin == null) {
                return emptyList()
            }
            return listOf(ActionTarget.LocationTarget(origin))
        }
    }

    /** 读取本次执行内已保存的目标缓存。 */
    object TargetFinder : ActionTargeter {

        override val id = "target.finder"

        override val aliases = listOf("target.finder")

        override fun validate(arguments: ActionArguments) {
            arguments.require("id")
            arguments.rejectUnknown("id")
        }

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val key = arguments.requireText(scope, "id")
            val cached = TargetCache.load(scope, key)
            if (cached == null) {
                throw IllegalArgumentException(
                    "${arguments.path} references target cache '$key' that was never stored.",
                )
            }
            val result = ArrayList<ActionTarget>()
            for (target in cached) {
                if (target.isActive()) {
                    result.add(target)
                }
            }
            return result
        }
    }

    /** 以施法者准星指向的首个实体为目标。 */
    object Crosshair : ActionTargeter {

        override val id = "crosshair"

        override val aliases = listOf("crosshair")

        override val scope = ActionScope.SKILL

        override fun validate(arguments: ActionArguments) {
            arguments.require("range")
            arguments.rejectUnknown("range")
        }

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val sender = scope.sender
            if (sender !is Player) {
                return emptyList()
            }
            val range = arguments.requireNumber(scope, "range")
            if (range <= 0.0) {
                return emptyList()
            }
            val eye = sender.eyeLocation
            val direction = eye.direction
            val trace = sender.world.rayTraceEntities(eye, direction, range, CROSSHAIR_RAY_SIZE)
            if (trace == null) {
                return emptyList()
            }
            val entity = trace.hitEntity
            if (entity !is LivingEntity) {
                return emptyList()
            }
            if (!entity.isValid || entity.isDead) {
                return emptyList()
            }
            return listOf(ActionTarget.EntityTarget(entity))
        }
    }

    /** 只保留非友方目标。 */
    object Enemy : ActionTargeter {

        override val id = "enemy"

        override val aliases = listOf("enemy")

        override val scope = ActionScope.SKILL

        override fun validate(arguments: ActionArguments) {
            arguments.rejectUnknown("los")
        }

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val source = scope.sender
            if (source !is LivingEntity) {
                return emptyList()
            }
            val requireLineOfSight = arguments.boolean(scope, "los") ?: false
            val result = ArrayList<ActionTarget>()
            for (target in input) {
                val entity = target.bukkitEntity()
                if (entity == null) {
                    continue
                }
                if (entity.uniqueId == source.uniqueId) {
                    continue
                }
                if (!entity.isValid || entity.isDead) {
                    continue
                }
                if (ActionFactionPolicy.isFriendly(source, entity)) {
                    continue
                }
                if (requireLineOfSight && !source.hasLineOfSight(entity)) {
                    continue
                }
                result.add(target)
            }
            return result
        }
    }

    /** 只保留友方目标。 */
    object Ally : ActionTargeter {

        override val id = "ally"

        override val aliases = listOf("ally")

        override val scope = ActionScope.SKILL

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val source = scope.sender
            if (source !is LivingEntity) {
                return emptyList()
            }
            val result = ArrayList<ActionTarget>()
            for (target in input) {
                val entity = target.bukkitEntity()
                if (entity == null) {
                    continue
                }
                if (!entity.isValid || entity.isDead) {
                    continue
                }
                if (!ActionFactionPolicy.isFriendly(source, entity)) {
                    continue
                }
                result.add(target)
            }
            return result
        }
    }

    /** 只保留指定实体类型。 */
    object Type : ActionTargeter {

        override val id = "type"

        override val aliases = listOf("type")

        override fun validate(arguments: ActionArguments) {
            arguments.require("id")
            arguments.rejectUnknown("id")
        }

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val names = arguments.list(scope, "id")
            val types = ArrayList<org.bukkit.entity.EntityType>()
            for (name in names) {
                var matched: org.bukkit.entity.EntityType? = null
                for (candidate in org.bukkit.entity.EntityType.values()) {
                    if (candidate.name.equals(name, true)) {
                        matched = candidate
                        break
                    }
                }
                if (matched == null) {
                    throw IllegalArgumentException("${arguments.path} references unknown entity type '$name'.")
                }
                types.add(matched)
            }
            val result = ArrayList<ActionTarget>()
            for (target in input) {
                val entity = target.bukkitEntity()
                if (entity == null) {
                    continue
                }
                if (types.contains(entity.type)) {
                    result.add(target)
                }
            }
            return result
        }
    }

    /** 排除指定实体类型。 */
    object ExcludeType : ActionTargeter {

        override val id = "exclude-type"

        override val aliases = listOf("exclude-type")

        override fun validate(arguments: ActionArguments) {
            arguments.require("id")
            arguments.rejectUnknown("id")
        }

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val names = arguments.list(scope, "id")
            val types = ArrayList<org.bukkit.entity.EntityType>()
            for (name in names) {
                var matched: org.bukkit.entity.EntityType? = null
                for (candidate in org.bukkit.entity.EntityType.values()) {
                    if (candidate.name.equals(name, true)) {
                        matched = candidate
                        break
                    }
                }
                if (matched == null) {
                    throw IllegalArgumentException("${arguments.path} references unknown entity type '$name'.")
                }
                types.add(matched)
            }
            val result = ArrayList<ActionTarget>()
            for (target in input) {
                val entity = target.bukkitEntity()
                if (entity == null) {
                    continue
                }
                if (!types.contains(entity.type)) {
                    result.add(target)
                }
            }
            return result
        }
    }

    /** 按名称正则保留目标。 */
    object Name : ActionTargeter {

        override val id = "name"

        override val aliases = listOf("name")

        override fun validate(arguments: ActionArguments) {
            arguments.require("pattern")
            arguments.rejectUnknown("pattern")
        }

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val patterns = ArrayList<Regex>()
            for (raw in arguments.list(scope, "pattern")) {
                patterns.add(Regex(raw, RegexOption.IGNORE_CASE))
            }
            val result = ArrayList<ActionTarget>()
            for (target in input) {
                val entity = target.bukkitEntity()
                if (entity == null) {
                    continue
                }
                for (pattern in patterns) {
                    if (pattern.containsMatchIn(entity.name)) {
                        result.add(target)
                        break
                    }
                }
            }
            return result
        }
    }

    /** 按世界名保留目标。 */
    object World : ActionTargeter {

        override val id = "world"

        override val aliases = listOf("world")

        override fun validate(arguments: ActionArguments) {
            arguments.require("name")
            arguments.rejectUnknown("name")
        }

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val names = ArrayList<String>()
            for (item in arguments.list(scope, "name")) {
                names.add(item.lowercase())
            }
            val result = ArrayList<ActionTarget>()
            for (target in input) {
                val entity = target.bukkitEntity()
                if (entity == null) {
                    continue
                }
                if (names.contains(entity.world.name.lowercase())) {
                    result.add(target)
                }
            }
            return result
        }
    }

    /** 把施法者自身并入容器。 */
    object IncludeSelf : ActionTargeter {

        override val id = "include-self"

        override val aliases = listOf("include-self")

        override val scope = ActionScope.SKILL

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val self = toTarget(scope.sender)
            if (self == null) {
                return input
            }
            val result = ArrayList<ActionTarget>(input)
            for (target in input) {
                if (target.bukkitEntity() != null && target.bukkitEntity()!!.uniqueId == scope.sender.let { sender ->
                        if (sender is LivingEntity) {
                            sender.uniqueId
                        } else {
                            null
                        }
                    }) {
                    return result
                }
            }
            result.add(self)
            return result
        }
    }

    /** 过滤掉失效或死亡的目标。 */
    object Alive : ActionTargeter {

        override val id = "alive"

        override val aliases = listOf("alive")

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val result = ArrayList<ActionTarget>()
            for (target in input) {
                if (target.isActive()) {
                    result.add(target)
                }
            }
            return result
        }
    }

    /** 按距离、名称或随机排序。 */
    object Sort : ActionTargeter {

        override val id = "sort"

        override val aliases = listOf("sort")

        override val scope = ActionScope.SKILL

        override fun validate(arguments: ActionArguments) {
            arguments.require("by")
            arguments.rejectUnknown("by")
        }

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val by = arguments.requireText(scope, "by").lowercase()
            if (by == "distance") {
                val origin = originOf(scope)
                if (origin == null) {
                    return input
                }
                val sorted = input.sortedBy { target -> distanceOf(target, origin) }
                return ArrayList(sorted)
            }
            if (by == "name") {
                val sorted = input.sortedBy { target -> nameOf(target) }
                return ArrayList(sorted)
            }
            if (by == "random") {
                val shuffled = input.shuffled()
                return ArrayList(shuffled)
            }
            throw IllegalArgumentException("${arguments.path} sort must be distance, name or random.")
        }
    }

    /** 反转当前顺序。 */
    object Reverse : ActionTargeter {

        override val id = "reverse"

        override val aliases = listOf("reverse")

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val result = ArrayList(input)
            result.reverse()
            return result
        }
    }

    /** 随机打乱当前顺序。 */
    object Shuffle : ActionTargeter {

        override val id = "shuffle"

        override val aliases = listOf("shuffle")

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val result = ArrayList(input)
            result.shuffle()
            return result
        }
    }

    /** 保留前 N 个目标。 */
    object Limit : ActionTargeter {

        override val id = "limit"

        override val aliases = listOf("limit")

        override fun validate(arguments: ActionArguments) {
            arguments.require("n")
            arguments.rejectUnknown("n")
        }

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val limit = arguments.requireInteger(scope, "n")
            if (limit <= 0) {
                return emptyList()
            }
            if (input.size <= limit) {
                return input
            }
            val result = ArrayList<ActionTarget>()
            for (index in 0 until limit) {
                result.add(input[index])
            }
            return result
        }
    }

    /**
     * 以全部在线玩家为容器。
     *
     * 供被动类流程使用：定时器触发时没有「当前目标」，需要自己遍历在线玩家。
     *
     * @property exclude-self 是否排除触发本次执行的玩家。
     */
    object OnlinePlayers : ActionTargeter {

        override val id = "online-players"

        override val aliases = listOf("online-players")

        override fun validate(arguments: ActionArguments) {
            arguments.rejectUnknown("exclude-self")
        }

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val excludeSelf = arguments.boolean(scope, "exclude-self") ?: false
            val sender = scope.sender
            val result = ArrayList<ActionTarget>()
            for (player in org.bukkit.Bukkit.getOnlinePlayers()) {
                if (excludeSelf && player == sender) {
                    continue
                }
                val target = toTarget(player)
                if (target == null) {
                    continue
                }
                result.add(target)
            }
            return result
        }
    }

    /**
     * 以事件载荷里的实体为容器。
     *
     * 触发器派发时会把事件相关实体放进载荷（例如 `defender` / `attacker`），
     * 用这一项就能指到「本次事件的另一方」，不必为每种事件单开目标器。
     *
     * @property key 载荷键名。
     */
    object Payload : ActionTargeter {

        override val id = "payload"

        override val aliases = listOf("payload")

        override fun validate(arguments: ActionArguments) {
            arguments.require("key")
            arguments.rejectUnknown("key")
        }

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val key = arguments.requireText(scope, "key")
            val value = scope.payload[key]
            val target = toTarget(value)
            if (target == null) {
                return emptyList()
            }
            return listOf(target)
        }
    }

    /**
     * 按 `mode` 合并源目标器结果。
     *
     * @param scope 查询上下文。
     * @param arguments 目标器参数。
     * @param input 上游容器。
     * @param found 本次选出的容器。
     * @return 合并后的容器。
     */
    private fun merge(
        scope: QueryScope,
        arguments: ActionArguments,
        input: List<ActionTarget>,
        found: List<ActionTarget>,
    ): List<ActionTarget> {
        val mode = arguments.text(scope, "mode") ?: MODE_REPLACE
        if (mode.equals(MODE_REPLACE, true)) {
            return found
        }
        if (!mode.equals(MODE_ADD, true)) {
            throw IllegalArgumentException("${arguments.path} mode must be replace or add.")
        }
        val result = ArrayList<ActionTarget>(input)
        for (target in found) {
            var exists = false
            val entity = target.bukkitEntity()
            for (existing in result) {
                if (entity != null && existing.bukkitEntity() != null && existing.bukkitEntity()!!.uniqueId == entity.uniqueId) {
                    exists = true
                    break
                }
            }
            if (!exists) {
                result.add(target)
            }
        }
        return result
    }

    /**
     * 把施法者转换为目标。
     *
     * @param sender 施法者。
     * @return 目标；无法转换时返回 null。
     */
    private fun toTarget(sender: Any?): ActionTarget? {
        if (sender is Player) {
            return ActionTarget.PlayerTarget(sender)
        }
        if (sender is LivingEntity) {
            return ActionTarget.EntityTarget(sender)
        }
        if (sender is Location) {
            return ActionTarget.LocationTarget(sender)
        }
        return null
    }

    /**
     * 取施法者位置。
     *
     * @param scope 查询上下文。
     * @return 位置；施法者不可用时返回 null。
     */
    private fun originOf(scope: QueryScope): Location? {
        val sender = scope.sender
        if (sender is LivingEntity) {
            return sender.location
        }
        if (sender is Location) {
            return sender
        }
        return null
    }

    /**
     * 解析选区朝向。
     *
     * @param source 施法者。
     * @param override 显式覆盖的朝向。
     * @param origin 施法者位置，作为最终回退。
     * @return 世界朝向。
     */
    private fun resolveYaw(source: LivingEntity, override: Double?, origin: Location): Float {
        if (override != null) {
            return override.toFloat()
        }
        return EntityFacingProviders.getFacingYaw(source)
    }

    /**
     * 按朝向偏移选区中心。
     *
     * @param origin 原始中心。
     * @param yaw 世界朝向。
     * @param offsetX 右方向偏移。
     * @param offsetY 上方向偏移。
     * @param offsetZ 前方向偏移。
     * @return 偏移后的中心。
     */
    private fun offsetCenter(
        origin: Location,
        yaw: Float,
        offsetX: Double,
        offsetY: Double,
        offsetZ: Double,
    ): Location {
        val radians = Math.toRadians(yaw.toDouble())
        val forwardX = -sin(radians)
        val forwardZ = cos(radians)
        val rightX = cos(radians)
        val rightZ = sin(radians)
        val center = origin.clone()
        center.add(offsetZ * forwardX + offsetX * rightX, offsetY, offsetZ * forwardZ + offsetX * rightZ)
        return center
    }

    /**
     * 从候选实体中筛出存活的生物，并排除施法者自身。
     *
     * @param candidates 候选实体。
     * @param source 施法者。
     * @return 生物列表。
     */
    private fun collectLiving(candidates: Collection<Entity>, source: LivingEntity): List<Entity> {
        val result = ArrayList<Entity>()
        for (candidate in candidates) {
            if (candidate !is LivingEntity) {
                continue
            }
            if (candidate.uniqueId == source.uniqueId) {
                continue
            }
            result.add(candidate)
        }
        return result
    }

    /**
     * 计算目标到原点的距离。
     *
     * @param target 目标。
     * @param origin 原点。
     * @return 距离；无法计算时返回最大值。
     */
    private fun distanceOf(target: ActionTarget, origin: Location): Double {
        val entity = target.bukkitEntity()
        if (entity == null) {
            return Double.MAX_VALUE
        }
        return entity.location.distance(origin)
    }

    /**
     * 取目标名称，用于名称排序。
     *
     * @param target 目标。
     * @return 名称。
     */
    private fun nameOf(target: ActionTarget): String {
        val entity = target.bukkitEntity()
        if (entity == null) {
            return ""
        }
        return entity.name
    }
}
