package com.gitee.planners.core.config

import com.gitee.planners.api.RegistryBuiltin
import com.gitee.planners.api.job.Condition
import com.gitee.planners.api.job.Job
import com.gitee.planners.api.job.Route
import com.gitee.planners.api.job.Router
import com.gitee.planners.api.script.KetherScript
import com.gitee.planners.api.script.KetherScriptOptions
import org.bukkit.entity.Player
import taboolib.library.configuration.ConfigurationSection

class ImmutableRoute(private val parent: Router, private val config: ConfigurationSection) : Route {

    val routerId = parent.id

    override val id = config.name

    private val branchs = config.getStringList("branches")

    val condition = Condition.combined(config.getConfigurationSection("condition"))

    override fun getBranches(): List<Route> {
        return branchs.mapNotNull { parent.getRouteOrNull(it) }
    }

    override fun getJob(): Job {
        return RegistryBuiltin.JOB.getOrNull(id) ?: error("Couldn't find job with id $id")
    }

    override fun isInfer(player: Player, options: KetherScriptOptions): Condition.VerifyInfo {
        return condition.verify(KetherScriptOptions.sender(player, options))
    }

}
