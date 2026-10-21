package com.gitee.planners.core.condition

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerRouter
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.core.skilltree.SkillTreeNodeEffectService
import com.gitee.planners.api.item.ItemProviders
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.function.warning
import java.util.UUID
import java.util.concurrent.CompletableFuture

/** 原生学习条件模板，按 type 选择固定的检查和扣除实现。 */
class NativeConditionConfig(
    key: String,
    private val type: String,
    props: Map<String, Any>,
    hint: String,
    private val defaultConsume: Boolean,
) : ConditionConfig(key, props, hint, defaultConsume) {
    override val preparationKey: String = "native:$type"

    override fun validateProperties(properties: Map<String, Any>) {
        NativeRequirement.parse(type, properties)
    }

    override fun prepare(playerId: UUID): CompletableFuture<ConditionExecution> {
        return CompletableFuture.completedFuture(Execution())
    }

    private inner class Execution : ConditionExecution {
        override fun evaluate(
            player: Player,
            profile: PlayerTemplate,
            router: PlayerRouter?,
            route: PlayerRoute?,
            conditionProps: Map<String, Any>
        ): Boolean {
            return NativeRequirement.check(type, player, profile, router, route, conditionProps)
        }

        override fun evaluateBatchPersistent(
            player: Player,
            profile: PlayerTemplate,
            router: PlayerRouter?,
            route: PlayerRoute?,
            conditionInputs: List<Map<String, Any>>
        ): ConditionExecution.BatchEvaluation {
            val encoded = StringBuilder()
            for (input in conditionInputs) {
                val rawProps = input["props"]
                require(rawProps is Map<*, *>) { "原生条件 $key 缺少 props" }
                val properties = LinkedHashMap<String, Any>()
                for ((propertyKey, propertyValue) in rawProps) {
                    require(propertyKey is String && propertyValue != null) { "原生条件 $key 参数非法" }
                    properties[propertyKey] = propertyValue
                }
                if (NativeRequirement.check(type, player, profile, router, route, properties)) {
                    encoded.append('1')
                } else {
                    encoded.append('0')
                }
            }
            return ConditionExecution.BatchEvaluation(encoded.toString())
        }

        override fun prepareConsume(
            player: Player,
            profile: PlayerTemplate,
            router: PlayerRouter?,
            route: PlayerRoute?,
            conditionProps: Map<String, Any>
        ): LearningCost? {
            if (!defaultConsume) {
                return null
            }
            return NativeRequirement.cost(type, player, conditionProps)
        }
    }

    /** 原生条件的参数解析、检查和成本创建逻辑。 */
    private object NativeRequirement {
        fun parse(type: String, properties: Map<String, Any>) {
            when (type) {
                "level" -> requireNumber(properties, "min")
                "skill" -> {
                    requireString(properties, "skillId")
                    requireNumber(properties, "min")
                }
                "job" -> requireString(properties, "id")
                "skill_points" -> requireNumber(properties, "amount")
                "item" -> {
                    requireString(properties, "matcher")
                    requireNumber(properties, "amount")
                    ItemMatcher.parse(properties["matcher"].toString())
                }
                "command" -> requireString(properties, "command")
                "flag" -> require(properties["value"] is Boolean) { "原生条件参数 value 必须是 Boolean" }
                "node_any" -> {
                    requireString(properties, "treeId")
                    require(properties["nodes"] is List<*>) { "原生条件参数 nodes 必须是列表" }
                }
                else -> throw IllegalArgumentException("未知原生条件类型：$type")
            }
        }

        fun check(type: String, player: Player, profile: PlayerTemplate, router: PlayerRouter?, route: PlayerRoute?, properties: Map<String, Any>): Boolean {
            parse(type, properties)
            return when (type) {
                "level" -> profile.level >= number(properties, "min")
                "skill" -> SkillTreeNodeEffectService.getSkillLevel(profile, properties["skillId"].toString()) >= number(properties, "min")
                "job" -> router != null && router.getCurrentJob().id == properties["id"].toString()
                "skill_points" -> router != null && router.skillPointsCurrent >= number(properties, "amount")
                "item" -> ItemMatcher.count(player, properties["matcher"].toString()) >= number(properties, "amount")
                "command" -> true
                "flag" -> properties["value"] == true
                "node_any" -> {
                    val nodes = properties["nodes"] as List<*>
                    var matched = false
                    for (node in nodes) {
                        require(node is String) { "node_any.nodes 只能包含节点 ID" }
                        if (route != null && route.getNodeLevel(properties["treeId"].toString(), node) > 0) {
                            matched = true
                            break
                        }
                    }
                    matched
                }
                else -> false
            }
        }

        fun cost(type: String, player: Player, properties: Map<String, Any>): LearningCost? {
            return when (type) {
                "skill_points" -> SkillPointLearningCost(number(properties, "amount"))
                "item" -> ItemLearningCost(player, ItemMatcher.parse(properties["matcher"].toString()), number(properties, "amount"))
                "command" -> CommandLearningCost(player, properties["command"].toString(), properties)
                else -> null
            }
        }

        private fun requireString(properties: Map<String, Any>, key: String) {
            require(properties[key] is String && properties[key].toString().isNotBlank()) { "原生条件参数 $key 必须是非空字符串" }
        }

        private fun requireNumber(properties: Map<String, Any>, key: String) {
            val value = properties[key]
            require(value is Number && value.toInt() >= 0) { "原生条件参数 $key 必须是非负整数" }
        }

        private fun number(properties: Map<String, Any>, key: String): Int {
            return (properties[key] as Number).toInt()
        }
    }

    /** 物品 matcher，支持 provider:item-id 和 Minecraft 材质 ID。 */
    private class ItemMatcher private constructor(
        private val raw: String,
        private val material: Material?,
        private val providerId: String?,
        private val itemId: String?
    ) {

        fun matches(item: ItemStack): Boolean {
            if (item.type.isAir) {
                return false
            }
            val expected = material
            if (expected != null) {
                return item.type == expected
            }
            val provider = providerId!!
            if (provider == VANILLA_PROVIDER) {
                return item.type.key.toString() == raw
            }
            val library = ItemProviders.getOrNull(provider) ?: return missingProvider(provider)
            return library.matches(item, itemId!!)
        }

        companion object {

            /** 原版命名空间，其余 provider 段一律按物品库适配器处理。 */
            private const val VANILLA_PROVIDER = "minecraft"

            private val REFERENCE = Regex("[a-z0-9_.-]+:[a-z0-9_./-]+")

            /** 已告警的缺失物品库，避免每次求值重复刷屏。 */
            private val missingProviders = HashSet<String>()

            fun parse(raw: String): ItemMatcher {
                val value = raw.trim()
                require(value.isNotEmpty()) { "item matcher 不能为空" }
                val separator = value.indexOf(':')
                if (separator <= 0 || separator >= value.length - 1) {
                    val material = Material.matchMaterial(value)
                    require(material != null) { "item matcher 必须是 provider:item-id 或 Minecraft 材质 ID：$value" }
                    return ItemMatcher(value, material, null, null)
                }
                require(REFERENCE.matches(value)) { "item matcher 必须是 provider:item-id 或 Minecraft 材质 ID：$value" }
                return ItemMatcher(value, null, value.substring(0, separator), value.substring(separator + 1))
            }

            /** 物品库未注册属于配置与本服插件不匹配，条件按不满足处理并告警一次。 */
            private fun missingProvider(id: String): Boolean {
                if (missingProviders.add(id)) {
                    warning("物品库未注册：$id，引用该物品库的学习条件始终不满足")
                }
                return false
            }

            fun count(player: Player, raw: String): Int {
                val matcher = parse(raw)
                var result = 0
                for (item in player.inventory.storageContents) {
                    if (item != null && matcher.matches(item)) {
                        result += item.amount
                    }
                }
                return result
            }
        }

        fun count(player: Player): Int {
            return count(player, raw)
        }
    }

    /** 原子物品成本，扣除失败会抛出并恢复扣除前的背包快照。 */
    private class ItemLearningCost(
        private val player: Player,
        private val matcher: ItemMatcher,
        private val amount: Int
    ) : LearningCost {
        private val before = snapshot(player)
        private var committed = false

        override fun skillPoints(): Int {
            return 0
        }

        override fun commit() {
            if (committed) {
                return
            }
            var remaining = amount
            for (index in player.inventory.storageContents.indices) {
                if (remaining <= 0) {
                    break
                }
                val item = player.inventory.storageContents[index]
                if (item != null && matcher.matches(item)) {
                    val removed = minOf(item.amount, remaining)
                    item.amount -= removed
                    remaining -= removed
                    if (item.amount <= 0) {
                        player.inventory.setItem(index, null)
                    }
                }
            }
            check(remaining == 0) { "扣除物品失败：背包数量在校验后发生变化" }
            committed = true
        }

        override fun rollback() {
            if (!committed) {
                return
            }
            for (index in before.indices) {
                player.inventory.setItem(index, before[index])
            }
            committed = false
        }

        /** 捕获主背包槽位，回滚时只恢复本成本触及的库存区域。 */
        private fun snapshot(player: Player): Array<org.bukkit.inventory.ItemStack?> {
            val contents = player.inventory.storageContents
            val result = arrayOfNulls<org.bukkit.inventory.ItemStack>(contents.size)
            for (index in contents.indices) {
                val item = contents[index]
                if (item != null) {
                    result[index] = item.clone()
                }
            }
            return result
        }
    }

    /** 命令型成本，Bukkit 返回 false 或抛出异常时视为扣除失败。 */
    private class CommandLearningCost(
        private val player: Player,
        private val template: String,
        private val properties: Map<String, Any>
    ) : LearningCost {
        private var committed = false

        override fun skillPoints(): Int {
            return 0
        }

        override fun commit() {
            if (committed) {
                return
            }
            var command = template.replace("{player}", player.name)
            for ((key, value) in properties) {
                command = command.replace("{props.$key}", value.toString())
            }
            val success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            check(success) { "命令型条件扣除失败：$command" }
            committed = true
        }

        override fun rollback() {
            // 命令没有通用逆操作，失败时由命令自身保证原子性。
            committed = false
        }
    }
}
