package com.gitee.planners.api.common.entity

interface ProxyEntity<T> {

    fun getInstance() : T

    fun isDead() : Boolean

}