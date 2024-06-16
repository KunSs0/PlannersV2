package com.gitee.planners.api.job

import com.gitee.planners.api.common.Unique
import com.gitee.planners.core.config.level.Algorithm

interface Router : Unique {

    val name: String

    val algorithmLevel: Algorithm?

    fun getRouteOrNull(id: String): Route?

    fun getRouteByJob(job: Job): Route?

}
