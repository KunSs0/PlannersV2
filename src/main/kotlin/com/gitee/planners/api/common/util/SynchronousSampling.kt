package com.gitee.planners.api.common.util

interface SynchronousSampling<T> {

    fun get() : T

}
