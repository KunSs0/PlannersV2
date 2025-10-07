package com.gitee.planners.api.job.target

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.entity.ProxyBukkitEntity
import com.gitee.planners.api.common.metadata.EntityMetadataManager
import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.common.metadata.MetadataContainer
import com.gitee.planners.api.event.entity.EntityStateEvent
import com.gitee.planners.core.config.State
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
import kotlin.math.max

class TargetBukkitEntity(override val instance: Entity) : TargetEntity<Entity>, TargetCommandSender<Entity>,
    TargetContainerization {

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

        val metadataValue = this.instance.getMetaFirstOrNull("pl.state.${state.id}")
        return metadataValue?.asBoolean() == true
    }

    override fun isExpired(state: State): Boolean {
        val endAt = this.instance.getMetaFirstOrNull("pl.state.${state.id}.end")?.asLong() ?: return false
        return endAt > 0 && System.currentTimeMillis() >= endAt
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
            this.instance.setMeta("pl.state.${state.id}", true)
            this.instance.setMeta("pl.state.${state.id}.end", System.currentTimeMillis() + duration * 50)
            EntityStateEvent.Attach.Post(this, state).call()
            // 注册定时器任务
            var task = this.instance.getMetaFirstOrNull("pl.state.${state.id}.task")?.value() as? PlatformExecutor.PlatformTask
            if (task != null) {
                task.cancel()
            }
            info("Registered state ${state.id} timer task")
            task = submit(delay = duration, async = true) {
                this@TargetBukkitEntity.endState(state)
            }

            this.instance.setMeta("pl.state.${state.id}.task", task)
        }
    }

    private fun endState(state: State) {
        info("State ${state.id} timer task ended")
        if (EntityStateEvent.End(this, state).call()) {
            this.removeState(state)
        }
    }

    override fun removeState(state: State) {
        if (state.isStatic || !hasState(state)) {
            return
        }
        if (EntityStateEvent.Detach.Pre(this, state).call()) {
            this.instance.removeMeta("pl.state.${state.id}")
            this.instance.removeMeta("pl.state.${state.id}.end")
            this.instance.removeMeta("pl.state.${state.id}.task")
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