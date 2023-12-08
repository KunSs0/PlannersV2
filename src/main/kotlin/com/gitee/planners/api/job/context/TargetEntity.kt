package com.gitee.planners.api.job.context

import java.util.UUID

interface TargetEntity<T> : TargetLocation<T> {

    fun getUniqueId(): UUID

    fun getName(): String

}
