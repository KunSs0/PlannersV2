package com.gitee.planners.module.action

import com.gitee.planners.api.common.facing.EntityFacingProviders
import com.gitee.planners.api.common.util.RangeNearestEntityFinder
import com.gitee.planners.api.common.util.RectNearestEntityFinder
import com.gitee.planners.api.common.util.SectorNearestEntityFinder
import com.gitee.planners.module.action.query.QueryScope
import com.gitee.planners.module.compat.faction.EntityFriendlyPolicy
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.BoundingBox
import java.util.function.Predicate
import kotlin.math.cos
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

    /** 以技能施法者为容器；`self` 是同等语义的旧名。 */
    object Sender : ActionTargeter {

        override val id = "sender"

        override val aliases = listOf("sender", "self")

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val target = toTarget(scope.caster ?: scope.sender)
            if (target == null) {
                return emptyList()
            }
            return listOf(target)
        }
    }


    /** 技能施法者所在的坐标；`self.loc` 是同等语义的旧名。 */
    object SenderLocation : ActionTargeter {

        override val id = "sender.loc"

        override val aliases = listOf("sender.loc", "self.loc")

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val location = locationOf(scope.caster ?: scope.sender)
            if (location == null) {
                return emptyList()
            }
            return listOf(ActionTarget.LocationTarget(location))
        }
    }


    /**
     * 以本次技能的当前目标为容器。
     *
     * 单体技能不需要再写选区，直接用这一项；没有目标时容器为空，动作按空容器跳过。
     * 事件派发时没有绑定目标，改为读载荷里的 `target`：实体命中给实体，方块命中给坐标。
     */
    object Current : ActionTargeter {

        override val id = "target"

        override val aliases = listOf("target")

        override val scope = ActionScope.SKILL

        override val payloadKey = "target"

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val bound = toTarget(scope.target)
            if (bound != null) {
                if (!bound.isActive()) {
                    return emptyList()
                }
                return listOf(bound)
            }
            return payloadTarget(scope, payloadKey)
        }
    }

    /** 以施法者为中心按球形半径选取实体。 */
    object Range : ActionTargeter {

        override val id = "range"

        override val aliases = listOf("range")

        override val scope = ActionScope.SKILL

        override fun validate(arguments: ActionArguments) {
            arguments.require("radius")
            arguments.rejectUnknown("radius", "height", "mode")
        }

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val origin = originOf(scope)
            if (origin == null) {
                return emptyList()
            }
            val radius = arguments.requireNumber(scope, "radius")
            if (radius <= 0.0) {
                return emptyList()
            }
            // 给了 height 就按圆柱判定（水平半径 + 垂直半高），否则维持球形半径。
            val height = arguments.number(scope, "height")
            if (height != null && height <= 0.0) {
                return emptyList()
            }
            val world = origin.world
            if (world == null) {
                return emptyList()
            }
            val source = selfOf(scope)
            val finder = RangeNearestEntityFinder(origin, radius, height, emptyList())
            finder.samples = collectLiving(world, finder.samplingBounds(), source)
            val found = ArrayList<ActionTarget>()
            for (entity in finder.request()) {
                if (entity is LivingEntity) {
                    found.add(ActionTarget.EntityTarget(entity))
                }
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
            val origin = originOf(scope)
            if (origin == null) {
                return emptyList()
            }
            val source = selfOf(scope)
            val radius = arguments.requireNumber(scope, "radius")
            val angle = arguments.requireNumber(scope, "angle")
            if (radius <= 0.0 || angle <= 0.0) {
                return emptyList()
            }
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
            val finder = SectorNearestEntityFinder(center, angle, radius, directionYaw, emptyList())
            finder.samples = collectLiving(world, finder.samplingBounds(), source)
            val found = ArrayList<ActionTarget>()
            val matched = finder.request()
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
            val origin = originOf(scope)
            if (origin == null) {
                return emptyList()
            }
            val source = selfOf(scope)
            val width = arguments.requireNumber(scope, "width")
            val height = arguments.requireNumber(scope, "height")
            val length = arguments.requireNumber(scope, "length")
            if (width <= 0.0 || height <= 0.0 || length <= 0.0) {
                return emptyList()
            }
            val world = origin.world
            if (world == null) {
                return emptyList()
            }
            val offsetX = arguments.number(scope, "offsetX") ?: 0.0
            val offsetY = arguments.number(scope, "offsetY") ?: 0.0
            val offsetZ = arguments.number(scope, "offsetZ") ?: 0.0
            val directionYaw = resolveYaw(source, null, origin)
            val finder = RectNearestEntityFinder(
                origin,
                width,
                height,
                length,
                directionYaw,
                offsetX,
                offsetY,
                offsetZ,
                emptyList(),
            )
            finder.samples = collectLiving(world, finder.samplingBounds(), source)
            val matched = finder.request()
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
            arguments.rejectUnknown()
        }

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
                if (entity.uniqueId == source.uniqueId) {
                    continue
                }
                if (!entity.isValid || entity.isDead) {
                    continue
                }
                if (EntityFriendlyPolicy.isFriendly(source, entity)) {
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
                if (!EntityFriendlyPolicy.isFriendly(source, entity)) {
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

    /**
     * 按距离、名称、生命值、生命比例或随机排序。
     *
     * 数值键（`distance` / `health` / `health-ratio`）支持 `order=asc|desc`，缺省升序；
     * 读不到数值的目标（位置目标、无有效生命上限）在两种顺序下都排在最后，不参与比较。
     * `name` 与 `random` 不接受 `order`，避免写出不生效的配置。
     */
    object Sort : ActionTargeter {

        override val id = "sort"

        override val aliases = listOf("sort")

        override val scope = ActionScope.SKILL

        override fun validate(arguments: ActionArguments) {
            arguments.require("by")
            arguments.rejectUnknown("by", "order")
            val order = arguments.literalText("order")
            if (order == null) {
                return
            }
            val normalized = order.trim().lowercase()
            if (normalized != "asc" && normalized != "desc") {
                throw IllegalArgumentException("${arguments.path} order must be asc or desc.")
            }
            val by = arguments.literalText("by")
            if (by == null) {
                return
            }
            val key = by.trim().lowercase()
            if (key == "random" || key == "name") {
                throw IllegalArgumentException(
                    "${arguments.path} order 只对 distance / health / health-ratio 生效。",
                )
            }
        }

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            val by = arguments.requireText(scope, "by").lowercase()
            val descending = isDescending(scope, arguments)
            if (by == "distance") {
                val origin = originOf(scope)
                if (origin == null) {
                    return input
                }
                return sortByValue(input, descending) { target -> distanceValue(target, origin) }
            }
            if (by == "health") {
                return sortByValue(input, descending) { target -> healthValue(target) }
            }
            if (by == "health-ratio") {
                return sortByValue(input, descending) { target -> healthRatioValue(target) }
            }
            if (by == "name") {
                val sorted = input.sortedBy { target -> nameOf(target) }
                return ArrayList(sorted)
            }
            if (by == "random") {
                val shuffled = input.shuffled()
                return ArrayList(shuffled)
            }
            throw IllegalArgumentException(
                "${arguments.path} sort must be distance, name, health, health-ratio or random.",
            )
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
     * 本次事件的攻击方。
     *
     * 只有声明了 `attacker` 的触发器能用；用错触发器在加载期就会报错。
     */
    object Attacker : ActionTargeter {

        override val id = "attacker"

        override val aliases = listOf("attacker")

        override val payloadKey = "attacker"

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            return payloadTarget(scope, payloadKey)
        }
    }

    /**
     * 本次事件的承伤方。
     *
     * 只有声明了 `defender` 的触发器能用；用错触发器在加载期就会报错。
     */
    object Defender : ActionTargeter {

        override val id = "defender"

        override val aliases = listOf("defender")

        override val payloadKey = "defender"

        override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
            return payloadTarget(scope, payloadKey)
        }
    }

    /**
     * 取载荷里指定键对应的实体容器。
     *
     * @param scope 查询上下文。
     * @param key 载荷键名。
     * @return 单元素容器；载荷里没有可转换实体时返回空列表。
     */
    private fun payloadTarget(scope: QueryScope, key: String): List<ActionTarget> {
        val value = scope.payload[key]
        val target = toTarget(value)
        if (target == null) {
            return emptyList()
        }
        return listOf(target)
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
     * 取目标或实体的坐标。
     *
     * @param value 目标、实体或坐标。
     * @return 坐标；无法取得时返回 null。
     */
    private fun locationOf(value: Any?): Location? {
        if (value is ActionTarget.LocationTarget) {
            return value.location
        }
        if (value is ActionTarget) {
            val entity = value.bukkitEntity()
            if (entity == null) {
                return null
            }
            return entity.location
        }
        if (value is Location) {
            return value
        }
        if (value is Entity) {
            return value.location
        }
        return null
    }


    /**
     * 取施法者实体，用于排除自身与读取朝向。
     *
     * @param scope 查询上下文。
     * @return 施法者是生物时返回该生物，否则返回 null。
     */
    private fun selfOf(scope: QueryScope): LivingEntity? {
        val source = scope.sender
        if (source is LivingEntity) {
            return source
        }
        return null
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
     * 取选区原点：流程绑定的坐标优先，其次施法者。
     *
     * 绑定坐标来自 `@this.loc` 或定时任务冻结的目标，用于「以固定坐标为中心」的选区；
     * 没有绑定坐标时退回施法者，与既有行为一致。
     *
     * @param scope 查询上下文。
     * @return 原点；坐标与施法者都不可用时返回 null。
     */
    private fun originOf(scope: QueryScope): Location? {
        val bound = scope.target
        if (bound is Location) {
            return bound
        }
        if (bound is ActionTarget.LocationTarget) {
            return bound.location
        }
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
    private fun resolveYaw(source: LivingEntity?, override: Double?, origin: Location): Float {
        if (override != null) {
            return override.toFloat()
        }
        if (source == null) {
            return origin.yaw
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
     * 按取样包围盒收集候选生物，并排除施法者自身。
     *
     * 交给宿主按 AABB 相交过滤，避免把大范围内的掉落物、箭矢、盔甲架等非生物对象
     * 全部装进列表后再丢弃。
     *
     * @param world 世界。
     * @param bounds 取样包围盒。
     * @param source 施法者。
     * @return 生物列表。
     */
    private fun collectLiving(world: org.bukkit.World, bounds: BoundingBox, source: LivingEntity?): List<LivingEntity> {
        val predicate = Predicate<Entity> { candidate ->
            candidate is LivingEntity && (source == null || candidate.uniqueId != source.uniqueId)
        }
        val candidates = world.getNearbyEntities(bounds, predicate)
        val result = ArrayList<LivingEntity>(candidates.size)
        for (candidate in candidates) {
            if (candidate is LivingEntity) {
                result.add(candidate)
            }
        }
        return result
    }

    /**
     * 读取排序方向。
     *
     * @param scope 当前执行上下文。
     * @param arguments 目标器参数。
     * @return 降序时返回 true；缺省升序。
     */
    private fun isDescending(scope: QueryScope, arguments: ActionArguments): Boolean {
        val raw = arguments.text(scope, "order")
        if (raw == null) {
            return false
        }
        return raw.trim().lowercase() == "desc"
    }

    /**
     * 按数值键排序。
     *
     * 读不到数值的目标（位置目标、无有效生命上限）在升序与降序下都排在最后：
     * 缺数据不应当因为换方向而抢到队首。相同数值保持输入顺序，便于「同分按上游顺序」的稳定输出。
     *
     * @param input 上游容器。
     * @param descending 是否降序。
     * @param valueOf 目标到排序数值的映射，返回 null 表示该目标缺少排序数据。
     * @return 排好序的新容器。
     */
    private fun sortByValue(
        input: List<ActionTarget>,
        descending: Boolean,
        valueOf: (ActionTarget) -> Double?,
    ): List<ActionTarget> {
        val entries = ArrayList<SortEntry>(input.size)
        var index = 0
        for (target in input) {
            entries.add(SortEntry(index, valueOf(target), target))
            index += 1
        }
        entries.sortWith(Comparator { left, right ->
            val leftValue = left.value
            val rightValue = right.value
            if (leftValue == null && rightValue == null) {
                left.index.compareTo(right.index)
            } else if (leftValue == null) {
                1
            } else if (rightValue == null) {
                -1
            } else {
                val compared = leftValue.compareTo(rightValue)
                if (compared != 0) {
                    if (descending) {
                        -compared
                    } else {
                        compared
                    }
                } else {
                    left.index.compareTo(right.index)
                }
            }
        })
        val result = ArrayList<ActionTarget>(entries.size)
        for (entry in entries) {
            result.add(entry.target)
        }
        return result
    }

    /**
     * 计算目标到原点的距离，用于距离排序。
     *
     * @param target 目标。
     * @param origin 原点。
     * @return 距离；目标没有实体句柄时返回 null。
     */
    private fun distanceValue(target: ActionTarget, origin: Location): Double? {
        val entity = target.bukkitEntity()
        if (entity == null) {
            return null
        }
        return entity.location.distance(origin)
    }

    /**
     * 读取目标当前生命，用于生命值排序。
     *
     * @param target 目标。
     * @return 当前生命；没有实体句柄或生命上限无效时返回 null。
     */
    private fun healthValue(target: ActionTarget): Double? {
        val entity = target.bukkitEntity()
        if (entity == null) {
            return null
        }
        if (entity.maxHealth <= 0.0) {
            return null
        }
        return entity.health
    }

    /**
     * 读取目标生命比例，用于「缺血优先」这类排序。
     *
     * @param target 目标。
     * @return `当前生命 / 生命上限`；没有实体句柄或上限无效时返回 null。
     */
    private fun healthRatioValue(target: ActionTarget): Double? {
        val entity = target.bukkitEntity()
        if (entity == null) {
            return null
        }
        val maximum = entity.maxHealth
        if (maximum <= 0.0) {
            return null
        }
        return entity.health / maximum
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

    /**
     * 排序条目：带排序数值与原始序号。
     *
     * @property index 输入顺序，作为同值时的稳定次序键。
     * @property value 排序数值；null 表示缺少排序数据。
     * @property target 目标。
     */
    private class SortEntry(val index: Int, val value: Double?, val target: ActionTarget)
}
