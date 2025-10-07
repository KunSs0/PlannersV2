package com.gitee.planners.api.job.target

import com.gitee.planners.core.config.State
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

    /**
     * Check whether the entity currently holds the state.
     */
    fun hasState(state: State): Boolean

    /**
     * Determine whether the state is expired.
     */
    fun isExpired(state: State): Boolean

    /**
     * Attach a state to the entity.
     *
     * @param state State definition
     * @param duration Duration in milliseconds, negative for infinite
     */
    fun addState(state: State, duration: Long = -1, coverBefore: Boolean)

    /**
     * Remove a state from the entity.
     */
    fun removeState(state: State)
}
