package com.gitee.planners.api.job.target

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.core.config.State
import org.bukkit.Location
import org.bukkit.entity.EntityType
import java.util.UUID

interface TargetEntity<T> : TargetLocation<T>,Target.Named {

    fun getUniqueId(): UUID

    fun getEntityType() : EntityType

    fun getBukkitEyeLocation() : Location

    /**
     * 是否拥有状态
     *
     * @param state 状态
     */
    fun hasState(state: State): Boolean

    /**
     * 添加状态
     *
     * @param state 状态
     */
    fun addState(state: State)

    /**
     * 移除状态
     *
     * @param state 状态
     */
    fun removeState(state: State)
}
