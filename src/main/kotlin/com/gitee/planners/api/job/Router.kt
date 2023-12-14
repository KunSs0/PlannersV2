package com.gitee.planners.api.job

import com.gitee.planners.api.common.Unique

interface Router : Unique {

    val name: String

    fun getRouteOrNull(id: String): Route?

    fun getRouteByJob(job: Job) : Route?

}
