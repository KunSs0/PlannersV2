package com.gitee.planners.core.config

import com.gitee.planners.api.Registries
import org.bukkit.inventory.ItemStack
import taboolib.library.configuration.ConfigurationSection
import taboolib.library.xseries.getItemStack

class ImmutableRoute(private val parent: ImmutableRouter, private val config: ConfigurationSection) {

    val routerId = parent.id

    val id = config.name

    val icon = config.getItemStack("icon")
        @JvmName("icon0")
        get

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

}
