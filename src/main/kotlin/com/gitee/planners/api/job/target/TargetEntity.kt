package com.gitee.planners.api.job.target

import com.gitee.planners.api.common.metadata.Metadata
import java.util.UUID

interface TargetEntity<T> : TargetLocation<T> {

    fun getUniqueId(): UUID

    fun getName(): String

    fun getMetadata(id: String): Metadata?

    fun setMetadata(id: String,data: Metadata)

}
