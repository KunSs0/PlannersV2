package com.gitee.planners.api.job.target

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.entity.ProxyBukkitEntity
import com.gitee.planners.api.common.metadata.EntityMetadataManager
import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.common.metadata.MetadataContainer
import com.gitee.planners.api.common.metadata.metadataValue
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
import taboolib.common.platform.function.warning
import taboolib.common.util.Vector
import java.util.*

class TargetBukkitEntity(override val instance: Entity) :
    TargetEntity<Entity>,
    TargetCommandSender<Entity>,
    TargetContainerization,
    CapableState {

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
            .getNearbyEntities(instance.location, vector.x, vector.y, vector.z)
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
        val holder = TargetStateHolder.parse(getMetadata(state.path()))
        return holder?.let { it.isValid && it.layer > 0 } ?: false
    }

    override fun isExpired(state: State): Boolean {
        val holder = TargetStateHolder.parse(getMetadata(state.path()))
        return holder?.isExpired ?: true
    }

    override fun attachState(state: State, duration: Long, refreshDuration: Boolean) {
        if (duration <= 0) {
            warning("\u72b6\u6001 ${state.id} \u7684\u6301\u7eed\u65f6\u95f4\u5fc5\u987b\u5927\u4e8e 0")
            return
        }
        val key = state.path()
        val holder = TargetStateHolder.parse(getMetadata(key))
        val hasValidHolder = holder != null && holder.isValid && holder.layer > 0

        val maxLayer = state.maxLayer.coerceAtLeast(1)
        if (hasValidHolder && holder!!.layer >= maxLayer && !refreshDuration) {
            return
        }

        val isFirstLayer = !hasValidHolder
        if (isFirstLayer && !EntityStateEvent.Mount.Pre(this, state).call()) {
            return
        }
        if (!EntityStateEvent.Attach.Pre(this, state).call()) {
            return
        }

        if (isFirstLayer) {
            holder?.close()
            val newHolder = TargetStateHolder.create(state, duration) {
                this@TargetBukkitEntity.endState(state)
            }
            newHolder.init()
            setMetadata(key, metadataValue(newHolder))
            EntityStateEvent.Attach.Post(this, state).call()
            EntityStateEvent.Mount.Post(this, state).call()
            return
        }

        val existingHolder = holder!!
        val incremented = existingHolder.incrementLayer(maxLayer)
        if (refreshDuration) {
            existingHolder.refresh(duration)
        }
        EntityStateEvent.Attach.Post(this, state).call()

        if (!incremented && !refreshDuration && existingHolder.layer >= maxLayer) {
            return
        }
    }

    private fun endState(state: State) {
        info("State ${state.id} timer task ended")
        if (EntityStateEvent.End(this, state).call()) {
            removeState(state)
        }
    }

    override fun detachState(state: State, layer: Int) {
        if (state.isStatic) {
            return
        }
        val key = state.path()
        val holder = TargetStateHolder.parse(getMetadata(key)) ?: return

        if (holder.layer <= 0) {
            setMetadata(key, metadataValue(null))
            return
        }

        val removal = if (layer >= 999) holder.layer.coerceAtLeast(1) else layer.coerceAtLeast(1)
        val finalRemoval = removal >= holder.layer

        if (!EntityStateEvent.Detach.Pre(this, state).call()) {
            return
        }

        if (!finalRemoval) {
            holder.decrementLayer(removal)
            EntityStateEvent.Detach.Post(this, state).call()
            return
        }

        if (!performFullRemoval(state, holder, key)) {
            return
        }
        EntityStateEvent.Detach.Post(this, state).call()
        EntityStateEvent.Close.Post(this, state).call()
    }

    override fun removeState(state: State) {
        if (state.isStatic) {
            return
        }
        val key = state.path()
        val holder = TargetStateHolder.parse(getMetadata(key)) ?: return

        if (holder.layer <= 0) {
            setMetadata(key, metadataValue(null))
            return
        }

        if (!EntityStateEvent.Detach.Pre(this, state).call()) {
            return
        }
        if (!performFullRemoval(state, holder, key)) {
            return
        }
        EntityStateEvent.Detach.Post(this, state).call()
        EntityStateEvent.Close.Post(this, state).call()
    }

    private fun performFullRemoval(state: State, holder: TargetStateHolder, key: String): Boolean {
        if (!EntityStateEvent.Close.Pre(this, state).call()) {
            return false
        }
        holder.close()
        setMetadata(key, metadataValue(null))
        return true
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







