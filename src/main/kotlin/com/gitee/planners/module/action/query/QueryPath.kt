package com.gitee.planners.module.action.query

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import java.util.Locale

/**
 * 一条已解析的查询路径。
 *
 * 路径由根节点与若干分段组成；分段语义按根节点分派，
 * 例如 `data` 的剩余分段整体作为 key，`node` 要求恰好两段。
 *
 * @property root 根节点。
 * @property segments 根节点之后的分段。
 * @property source 原始配置文本，用于错误信息。
 */
class QueryPath private constructor(
    val root: QueryRoot,
    val segments: List<String>,
    val source: String,
) {

    /**
     * 求值当前路径。
     *
     * @param scope 查询上下文。
     * @return 读取到的值；缺失时返回 null。
     * @throws IllegalArgumentException 分段数量不合法时抛出。
     */
    fun resolve(scope: QueryScope): Any? {
        return when (root) {
            QueryRoot.SENDER -> resolveSender(scope)
            QueryRoot.TARGET -> resolveTarget(scope)
            QueryRoot.SKILL -> resolveSkill(scope)
            QueryRoot.PROFILE -> resolveProfile(scope)
            QueryRoot.DATA -> queryData(scope)
            QueryRoot.ATTR -> queryAttr(scope)
            QueryRoot.NATURAL -> queryNatural(scope)
            QueryRoot.NODE -> queryNode(scope)
            QueryRoot.PLAYBACK -> queryPlayback(scope)
        }
    }

    /**
     * 读取目标字段。
     *
     * `anchored` 是计算字段：判断施法者是否为该目标的当前仇恨目标，
     * 由宿主注入的读取器给出；
     * `skill.<id>` 读取该目标玩家自己的技能学习等级，供被动流程逐玩家判定；
     * 其余字段按实体属性读取。
     */
    private fun resolveTarget(scope: QueryScope): Any? {
        val field = segments.firstOrNull()
        if (field != null && field.equals("anchored", true)) {
            return scope.anchoredReader(scope.target, scope.sender)
        }
        if (field != null && field.equals("skill", true) && segments.size >= 2) {
            return queryTargetSkill(scope.target, segments[1])
        }
        return resolveEntityField(scope.target)
    }

    /**
     * 读取目标玩家某个技能的学习等级。
     *
     * 只读在线且档案已加载的玩家；其余情况返回 0，让 `> 0` 这类判断自然不成立。
     *
     * @param target 目标实体。
     * @param skillId 技能 ID。
     * @return 技能等级；不可读时返回 0。
     */
    private fun queryTargetSkill(target: Any?, skillId: String): Int {
        val player = target as? Player
        if (player == null) {
            return 0
        }
        if (!player.isOnline || !player.isValid || player.isDead) {
            return 0
        }
        val template = com.gitee.planners.api.PlayerTemplateAPI.getOrNull(player.uniqueId)
        if (template == null) {
            return 0
        }
        return com.gitee.planners.core.skilltree.SkillTreeNodeEffectService.getSkillLevel(template, skillId)
    }

    /**
     * 读取本次连招播放的变量。
     *
     * 变量随播放销毁，不跨施法保留；没有播放上下文时返回 null。
     */
    private fun queryPlayback(scope: QueryScope): Any? {
        val key = segments.joinToString(".")
        if (key.isEmpty()) {
            return scope.payload
        }
        // 播放变量名本身包含点号，因此整段作为键名，不按路径分段。
        return scope.playbackReader(scope.payload, key)
    }

    /** 读取施法者字段。 */
    private fun resolveSender(scope: QueryScope): Any? {
        return resolveEntityField(scope.sender)
    }

    /** 读取实体或位置字段，按实际类型分派。 */
    private fun resolveEntityField(value: Any?): Any? {
        val field = segments.firstOrNull()
        if (field == null) {
            return value
        }
        if (value is Location) {
            return readLocation(value, field)
        }
        if (value is Player) {
            return readPlayer(value, field)
        }
        if (value is LivingEntity) {
            return readEntity(value, field)
        }
        if (value is Entity) {
            return readEntity(value, field)
        }
        return null
    }

    /** 读取玩家字段。 */
    private fun readPlayer(player: Player, field: String): Any? {
        return when (field) {
            "name" -> player.name
            "uuid" -> player.uniqueId.toString()
            "health" -> player.health
            "max_health" -> player.maxHealth
            "world" -> player.world.name
            "x" -> player.location.x
            "y" -> player.location.y
            "z" -> player.location.z
            "yaw" -> player.location.yaw.toDouble()
            "pitch" -> player.location.pitch.toDouble()
            else -> null
        }
    }

    /** 读取通用实体字段。 */
    private fun readEntity(entity: Entity, field: String): Any? {
        return when (field) {
            "name" -> entity.name
            "uuid" -> entity.uniqueId.toString()
            "world" -> entity.world.name
            "x" -> entity.location.x
            "y" -> entity.location.y
            "z" -> entity.location.z
            "yaw" -> entity.location.yaw.toDouble()
            "pitch" -> entity.location.pitch.toDouble()
            "health" -> {
                if (entity is LivingEntity) {
                    entity.health
                } else {
                    null
                }
            }

            "max_health" -> {
                if (entity is LivingEntity) {
                    entity.maxHealth
                } else {
                    null
                }
            }

            else -> null
        }
    }

    /** 读取位置字段。 */
    private fun readLocation(location: Location, field: String): Any? {
        return when (field) {
            "world" -> {
                if (location.world == null) {
                    null
                } else {
                    location.world?.name
                }
            }

            "x" -> location.x
            "y" -> location.y
            "z" -> location.z
            "yaw" -> location.yaw.toDouble()
            "pitch" -> location.pitch.toDouble()
            else -> null
        }
    }

    /** 读取技能字段与本次已计算的变量。 */
    private fun resolveSkill(scope: QueryScope): Any? {
        val field = segments.firstOrNull()
        if (field == null) {
            return scope.skill
        }
        if (scope.variables.containsKey(field)) {
            return scope.variables[field]
        }
        val key = field.lowercase(Locale.ROOT)
        if (key != field && scope.variables.containsKey(key)) {
            return scope.variables[key]
        }
        val skill = scope.skill
        if (skill == null) {
            return null
        }
        return scope.skillFieldReader(skill, key)
    }

    /** 读取玩家档案字段。 */
    private fun resolveProfile(scope: QueryScope): Any? {
        val field = segments.firstOrNull()
        if (field == null) {
            return scope.profile
        }
        val profile = scope.profile
        if (profile == null) {
            return null
        }
        return scope.profileFieldReader(profile, field.lowercase(Locale.ROOT))
    }

    /** 读取共享标记，剩余分段整体作为 key。 */
    private fun queryData(scope: QueryScope): Any? {
        if (segments.isEmpty()) {
            return null
        }
        val key = segments.joinToString(".")
        return scope.dataReader(key)
    }

    /** 读取战斗属性。 */
    private fun queryAttr(scope: QueryScope): Double? {
        val key = segments.firstOrNull()
        if (key == null) {
            return null
        }
        return scope.attrReader(key)
    }

    /** 读取职业自然成长。 */
    private fun queryNatural(scope: QueryScope): Double? {
        val key = segments.firstOrNull()
        if (key == null) {
            return null
        }
        return scope.naturalReader(key)
    }

    /** 读取技能树节点等级，要求两段。 */
    private fun queryNode(scope: QueryScope): Int? {
        if (segments.size != 2) {
            return null
        }
        val tree = segments[0]
        val node = segments[1]
        return scope.nodeReader(tree, node)
    }

    companion object {

        /**
         * 解析一段路径文本。
         *
         * @param text 已剥离 `<>` 外壳的路径文本。
         * @param label 配置路径，用于错误信息。
         * @return 解析得到的路径。
         * @throws IllegalArgumentException 根节点未知或分段数量不合法时抛出。
         */
        fun parse(text: String, label: String): QueryPath {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) {
                throw IllegalArgumentException("$label contains a blank query path.")
            }
            val parts = trimmed.split('.')
            val rootText = parts[0]
            val root = QueryRoot.of(rootText)
            if (root == null) {
                throw IllegalArgumentException(
                    "$label references unknown query root '$rootText', expected one of: ${QueryRoot.keywords()}",
                )
            }
            val segments = ArrayList<String>()
            for (index in 1 until parts.size) {
                val segment = parts[index]
                if (segment.isEmpty()) {
                    throw IllegalArgumentException("$label contains an empty segment in '$trimmed'.")
                }
                segments.add(segment)
            }
            if (root == QueryRoot.NODE && segments.size != 2) {
                throw IllegalArgumentException(
                    "$label node path '$trimmed' must be '<node.{tree}.{nodeId}>'.",
                )
            }
            if (segments.isEmpty() && root != QueryRoot.SENDER) {
                throw IllegalArgumentException("$label query path '$trimmed' requires at least one field.")
            }
            return QueryPath(root, segments, trimmed)
        }

        /**
         * 判断文本是否只由路径允许的字符组成。
         *
         * 用于加载期的纯路径快速判定，命中时不构造表达式。
         * 首字符必须是字母或下划线，避免把 `-1` 这类负数误判为路径。
         *
         * @param text 待判定文本。
         * @return 只含标识符、点、连字符与数字时返回 true。
         */
        fun isPlainPath(text: String): Boolean {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) {
                return false
            }
            val first = trimmed[0]
            if (!first.isLetter() && first != '_') {
                return false
            }
            for (character in trimmed) {
                val identifier = character.isLetterOrDigit() || character == '_' || character == '.' || character == '-'
                if (!identifier) {
                    return false
                }
            }
            return true
        }
    }
}