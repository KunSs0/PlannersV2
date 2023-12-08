package com.gitee.planners.api.job.context

interface Context {

    val trackId: String

    fun process()

}
