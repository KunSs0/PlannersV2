package com.gitee.planners.api.job.target

import com.gitee.planners.api.common.metadata.Metadata
import org.bukkit.entity.EntityType
import java.util.UUID

interface TargetEntity<T> : TargetLocation<T> {

    fun getUniqueId(): UUID

    fun getEntityType() : EntityType

    fun getName(): String


}
