package com.gitee.planners.api.job.target

import java.util.UUID

interface TargetEntity<T> : TargetLocation<T> {

    fun getUniqueId(): UUID

    fun getName(): String

}
