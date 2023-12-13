package com.gitee.planners.api.job.target

interface Target<T> {

    fun getInstance() : T

    companion object {


        inline fun <reified T : Target<*>> Target<*>.cast(): T {
            return this as T
        }

    }

}
