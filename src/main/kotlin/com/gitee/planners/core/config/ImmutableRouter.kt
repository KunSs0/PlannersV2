package com.gitee.planners.core.config

import com.gitee.planners.api.job.Job
import com.gitee.planners.api.job.Route
import com.gitee.planners.api.job.Router
import com.gitee.planners.core.config.level.Algorithm
import com.gitee.planners.util.mapSectionNotNull
import taboolib.library.xseries.getItemStack
import taboolib.module.configuration.Configuration

class ImmutableRouter(private val config: Configuration) : Router {

    override val id = config.file!!.nameWithoutExtension

    override val name = config.getString("__option__.name", id)!!

    override val algorithmLevel =
        Algorithm.parseKether(config.getConfigurationSection("__option__.algorithm.level"))

    val icon = config.getItemStack("__option__.icon")

    private val routes = config.mapSectionNotNull {
        // 加载所有阶段 过滤__option__
        if (it.name == "__option__") return@mapSectionNotNull null

        ImmutableRoute(this, it)
    }

    val originate = if (config.isString("__option__.originate")) {
        getRouteOrNull(config.getString("__option__.originate")!!) as? ImmutableRoute
    } else {
        routes.values.firstOrNull()
    }

    override fun getRouteOrNull(id: String): Route? {
        return routes[id]
    }

    override fun getRouteByJob(job: Job): Route? {
        return routes[job.id]
    }

}
