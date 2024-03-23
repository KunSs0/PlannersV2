package com.gitee.planners.api.job.target

interface Target<T> {

    val instance: T

    interface Named {

        fun getName(): String

    }

    companion object {


        inline fun <reified T : Target<*>> Target<*>.cast(): T? {
            return this as? T
        }

        inline fun <reified T : Target<*>> Target<*>.castUnsafely(): T {
            return cast<T>()!!
        }

    }

}
