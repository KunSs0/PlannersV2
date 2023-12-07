package com.gitee.planners.core.config

import com.gitee.planners.api.job.Job
import com.gitee.planners.api.job.Route
import com.gitee.planners.api.job.Router
import com.gitee.planners.util.mapSectionNotNull
import taboolib.module.configuration.Configuration

class ImmutableRouter(private val config: Configuration) : Router {

    override val id = config.file!!.nameWithoutExtension

    override val name = config.getString("__option__.name", id)!!

    val originate = config.getString("originate")

    private val routes = config.mapSectionNotNull {
        // 加载所有阶段 过滤__option__
        if (it.name == "__option__") return@mapSectionNotNull null

        ImmutableRoute(this,it)
    }

    override fun getRouteOrNull(id: String): Route? {
        return routes[id]
    }

    override fun getRouteByJob(job: Job): Route? {
        return routes[job.id]
    }

}
