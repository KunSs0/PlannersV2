package com.gitee.planners.core.config

import com.gitee.planners.api.common.Unique
import com.gitee.planners.core.config.level.Algorithm
import com.gitee.planners.util.mapSectionNotNull
import taboolib.library.xseries.getItemStack
import taboolib.module.configuration.Configuration

class ImmutableRouter(private val config: Configuration) : Unique {

    override val id = config.file!!.nameWithoutExtension

    val name = config.getString("__option__.name", id)!!

    val algorithmLevel =
        Algorithm.parse(config.getConfigurationSection("__option__.algorithm.level"))

    val icon = config.getItemStack("__option__.icon")

    private val routes = config.mapSectionNotNull {
        if (it.name == "__option__") return@mapSectionNotNull null
        ImmutableRoute(this, it)
    }

    val originate = if (config.isString("__option__.originate")) {
        getRouteOrNull(config.getString("__option__.originate")!!)
    } else {
        routes.values.firstOrNull()
    }

    fun getRouteOrNull(id: String): ImmutableRoute? {
        return routes[id]
    }

    fun getRouteByJob(job: ImmutableJob): ImmutableRoute? {
        return routes[job.id]
    }

}
