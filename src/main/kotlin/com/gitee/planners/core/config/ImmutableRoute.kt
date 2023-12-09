package com.gitee.planners.core.config

import com.gitee.planners.api.RegistryBuiltin
import com.gitee.planners.api.job.Condition
import com.gitee.planners.api.job.Job
import com.gitee.planners.api.job.Route
import com.gitee.planners.api.job.Router
import com.gitee.planners.api.common.script.KetherScriptOptions
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.configuration.ConfigurationSection
import taboolib.library.xseries.getItemStack

class ImmutableRoute(private val parent: Router, private val config: ConfigurationSection) : Route {

    val routerId = parent.id

    override val id = config.name

    val icon = config.getItemStack("icon")
        @JvmName("icon0")
        get

    private val branches = if (config.isString("branch")) {
        listOf(config.getString("branch")!!)
    } else {
        config.getStringList("branch")
    }

    val condition = Condition.combined(config.getConfigurationSection("condition"))

    override fun getBranches(): List<Route> {
        return branches.mapNotNull { parent.getRouteOrNull(it) }
    }

    override fun getIcon(): ItemStack? {
        return icon
    }

    override fun getJob(): Job {
        return RegistryBuiltin.JOB.getOrNull(id) ?: error("Couldn't find job with id $id")
    }

    override fun isInfer(player: Player, options: KetherScriptOptions): Condition.VerifyInfo {
        return condition.verify(KetherScriptOptions.sender(player, options))
    }

}
