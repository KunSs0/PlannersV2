package com.gitee.planners.api.job.target

import com.gitee.planners.api.common.metadata.Metadata

interface TargetContainerization  {

    fun getMetadata(id: String): Metadata?

    fun setMetadata(id: String,data: Metadata)

}
