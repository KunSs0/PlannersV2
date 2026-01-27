package com.gitee.planners.api.job.target

import org.bukkit.Location
import org.bukkit.entity.EntityType
import java.util.UUID

interface TargetEntity<T> : TargetLocation<T>, Target.Named {

    fun getUniqueId(): UUID

    fun getEntityType(): EntityType

    fun getBukkitEyeLocation(): Location

    /**
     * Whether the underlying entity is still valid.
     */
    fun isValid(): Boolean
}
