package com.gitee.planners.core.config

import com.gitee.planners.api.Registries
import com.gitee.planners.api.job.Job
import com.gitee.planners.api.job.Route
import com.gitee.planners.api.job.Router
import org.bukkit.inventory.ItemStack
import taboolib.library.configuration.ConfigurationSection
import taboolib.library.xseries.getItemStack

class ImmutableRoute(private val parent: Router, private val config: ConfigurationSection) : Route {

    val routerId = parent.id

    override val id = config.name

    val icon = config.getItemStack("icon")
        @JvmName("icon0")
        get

    /** 绑定的技能树 ID，null 表示无技能树 */
    val skillTree: String? = config.getString("skill.tree")

    private val branches = if (config.isString("branch")) {
        listOf(config.getString("branch")!!)
    } else {
        config.getStringList("branch")
    }

    override fun getBranches(): List<Route> {
        return branches.mapNotNull { parent.getRouteOrNull(it) }
    }

    override fun getIcon(): ItemStack? {
        return icon
    }

    override fun getJob(): Job {
        return Registries.JOB.getOrNull(id) ?: error("Couldn't find job with id $id")
    }

}
