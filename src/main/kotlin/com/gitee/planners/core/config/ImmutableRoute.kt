package com.gitee.planners.core.config

import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.Registries
import com.gitee.planners.module.action.query.QueryRef
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.configuration.ConfigurationSection
import taboolib.library.xseries.getItemStack
import java.util.Locale

class ImmutableRoute(private val parent: ImmutableRouter, private val config: ConfigurationSection) {

    val routerId = parent.id

    val id = config.name

    /**
     * 转职条件原文；为空表示无条件。
     *
     * 使用插值与求值表达式，例如 `economy.coin >= 500`。
     */
    val condition: String? = config.getString("condition")?.takeIf { it.isNotBlank() }

    /** 转职条件的预编译引用；语法非法时在加载期报错。 */
    private val conditionRef: QueryRef? = condition?.let {
        QueryRef.parseVariableExpression(it, "route:$id:condition")
    }

    /**
     * 判断该玩家是否满足转职条件。
     *
     * @param player 待转职的玩家。
     * @return 无条件或条件成立时返回 true。
     */
    fun isConditionSatisfied(player: Player): Boolean {
        val ref = conditionRef ?: return true
        val template = PlayerTemplateAPI.getOrNull(player.uniqueId) ?: return false
        val scope = QueryScope(
            sender = player,
            profile = template,
            label = "route:$id:condition",
        )
        return ref.resolveBoolean(scope)
    }

    /** 职业阶段图标的 namespaced ID。 */
    val iconItemId: String? = toItemId(config.getString("icon.material"))

    /**
     * 职业阶段 Bukkit 图标。
     *
     * 仅由传统 Bukkit GUI 按需创建，快照数据路径不需要 ItemStack。
     */
    val icon: ItemStack?
        @JvmName("getIconValue")
        get() {
            return config.getItemStack("icon")
        }

    /** 绑定的技能树 ID；一个职业阶段可绑定多个树。 */
    val skillTreeIds: List<String>
        get() {
            val values = config.getStringList("skill.trees")
            val result = ArrayList<String>()
            for (value in values) {
                if (value.isNotBlank()) {
                    result.add(value)
                }
            }
            return result
        }

    val branchIds = if (config.isString("branch")) {
        listOf(config.getString("branch")!!)
    } else {
        config.getStringList("branch")
    }

    fun getBranches(): List<ImmutableRoute> {
        return branchIds.mapNotNull { parent.getRouteOrNull(it) }
    }

    fun getIcon(): ItemStack? {
        return icon
    }

    fun getJob(): ImmutableJob {
        return Registries.JOB.getOrNull(id) ?: error("Couldn't find job with id $id")
    }

    /**
     * 将职业阶段图标材质文本转换为 Minecraft namespaced ID。
     *
     * @param material 配置中的 Bukkit Material 名称。
     * @return 小写 namespaced ID；未配置时返回 null。
     */
    private fun toItemId(material: String?): String? {
        if (material == null) {
            return null
        }
        val normalized = material.trim()
        if (normalized.isEmpty()) {
            return null
        }
        if (normalized.contains(':')) {
            return normalized.lowercase(Locale.ROOT)
        }
        return "minecraft:" + normalized.lowercase(Locale.ROOT)
    }

}
