package com.gitee.planners.core.config

import com.gitee.planners.api.RegistryBuiltin
import com.gitee.planners.api.job.Job
import com.gitee.planners.api.job.Route
import com.gitee.planners.api.job.Router
import com.gitee.planners.api.script.KetherScriptOptions
import org.bukkit.entity.Player
import taboolib.common5.cbool
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration
import taboolib.module.kether.runKether
import java.util.concurrent.CompletableFuture

class ImmutableRoute(private val parent: Router, private val config: ConfigurationSection) : Route {

    override val id = config.name
    override fun run(options: KetherScriptOptions): CompletableFuture<Any?> {
        TODO("Not yet implemented")
    }

    private val branchs = config.getStringList("branches")

    override fun getBranches(): List<Route> {
        return branchs.mapNotNull { parent.getRouteOrNull(it) }
    }

    override fun getJob(): Job {
        return RegistryBuiltin.JOB.getOrNull(id) ?: error("Couldn't find job with id $id")
    }

    override fun isInfer(player: Player, options: KetherScriptOptions): Boolean {
        return runKether(false) {
            this.match(player, options).thenApply { it.cbool }.getNow(false)
        }!! as Boolean
    }

}
