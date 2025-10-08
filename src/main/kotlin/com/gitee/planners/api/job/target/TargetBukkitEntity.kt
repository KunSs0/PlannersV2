package com.gitee.planners.api.job.target

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.entity.ProxyBukkitEntity
import com.gitee.planners.api.common.metadata.*
import com.gitee.planners.api.event.entity.EntityStateEvent
import com.gitee.planners.core.config.State
import com.gitee.planners.core.config.State.Companion.path
import com.gitee.planners.core.skill.entity.state.TargetStateHolder
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import taboolib.common.platform.function.info
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.warning
import taboolib.common.platform.service.PlatformExecutor
import taboolib.common.util.Vector
import taboolib.platform.util.*
import java.util.*

class TargetBukkitEntity(override val instance: Entity) : TargetEntity<Entity>, TargetCommandSender<Entity>,
    TargetContainerization, CapableState {

    override fun getUniqueId(): UUID {
        return instance.uniqueId
    }

    override fun getEntityType(): EntityType {
        return instance.type
    }

    override fun getName(): String {
        return instance.name
    }

    override fun getBukkitEyeLocation(): Location {
        return (instance as? LivingEntity)?.eyeLocation ?: getBukkitLocation()
    }

    override fun isValid(): Boolean {
        return instance.isValid
    }

    override fun getWorld(): String {
        return instance.world.name
    }

    override fun getBukkitWorld(): World? {
        return instance.world
    }

    override fun getBukkitLocation(): Location {
        return instance.location
    }

    override fun getX(): Double {
        return instance.location.x
    }

    override fun getY(): Double {
        return instance.location.y + instance.height / 2
    }

    override fun getZ(): Double {
        return instance.location.z
    }

    override fun add(x: Double, y: Double, z: Double) {
    }

    override fun sendMessage(message: String) {
        instance.sendMessage(message)
    }

    override fun dispatchCommand(command: String): Boolean {
        return Bukkit.dispatchCommand(instance, command)
    }

    override fun getNearbyLivingEntities(vector: Vector): List<LivingEntity> {
        return getBukkitWorld()!!
            .getNearbyEntities(this.instance.location, vector.x, vector.y, vector.z)
            .filterIsInstance<LivingEntity>()
    }

    private fun getMetadataContainer(): MetadataContainer {
        return if (instance is Player) {
            instance.plannersTemplate
        } else {
            EntityMetadataManager[ProxyBukkitEntity(instance)]
        }
    }

    override fun getMetadata(id: String): Metadata? {
        return getMetadataContainer()[id]
    }

    override fun setMetadata(id: String, data: Metadata) {
        getMetadataContainer()[id] = data
    }

    override fun hasState(state: State): Boolean {
        if (state.isStatic) {
            return true
        }

        return TargetStateHolder.parse(this.getMetadata(state.path())) != null
    }

    override fun isExpired(state: State): Boolean {
        val holder = TargetStateHolder.parse(this.getMetadata(state.path()))
        if (holder == null) {
            return true
        }

        return holder.isExpired
    }

    override fun addState(state: State, duration: Long, coverBefore: Boolean) {
        if (duration <= 0) {
            warning("The duration of state ${state.id} must be greater than 0")
            return
        }
        val alreadyHas = hasState(state)
        if (alreadyHas && !coverBefore) {
            return
        }
        if (EntityStateEvent.Attach.Pre(this, state).call()) {
            val holder = TargetStateHolder.parse(this.getMetadata(state.path()))
            // 移除旧状态
            if (holder != null) {
                holder.close()
            }
            val newHolder = TargetStateHolder.create(state, duration) {
                this@TargetBukkitEntity.endState(state)
            }
            newHolder.init()

            this.setMetadata(state.path(), metadataValue(newHolder))
            EntityStateEvent.Attach.Post(this, state).call()
        }
    }

    private fun endState(state: State) {
        info("State ${state.id} timer task ended")
        if (EntityStateEvent.End(this, state).call()) {
            this.removeState(state)
        }
    }

    override fun removeState(state: State) {
        if (state.isStatic) {
            return
        }
        val holder = TargetStateHolder.parse(this.getMetadata(state.path()))
        if (holder == null) {
            return
        }

        if (EntityStateEvent.Detach.Pre(this, state).call()) {
            this.setMetadata(state.path(), metadataValue(null))
            EntityStateEvent.Detach.Post(this, state).call()
        }
    }

    override fun toString(): String {
        return "TargetBukkitEntity(instance=$instance)"
    }

    override fun hashCode(): Int {
        return instance.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Target<*>

        return instance == other.instance
    }
}