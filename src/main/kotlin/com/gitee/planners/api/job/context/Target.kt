package com.gitee.planners.api.job.context

interface Target<T> {

    fun getInstance() : T

}
