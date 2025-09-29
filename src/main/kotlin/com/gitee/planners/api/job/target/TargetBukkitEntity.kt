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
import taboolib.common.util.Vector
import taboolib.platform.util.*
import java.util.*

class TargetBukkitEntity(override val instance: Entity) : TargetEntity<Entity>, TargetCommandSender<Entity>,
    TargetContainerization {

    private val stateMap = mutableMapOf<String, State>()

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
        }
        // 通过代理实体获取元数据容器
        else {
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
        if (metadataValue == null) {
            return false
        }

        return metadataValue.asBoolean()
    }

    override fun addState(state: State) {
        if (hasState(state)) {
            return
        }
        if (EntityStateEvent.Attach.Pre(this,state).call()) {
            this.instance.setMeta("pl.state.${state.id}", true)
            EntityStateEvent.Attach.Post(this,state).call()
        }

    }

    override fun removeState(state: State) {
        if (state.isStatic || !hasState(state)) {
            return
        }
        if (EntityStateEvent.Detach.Pre(this,state).call()) {
            this.instance.removeMeta("pl.state.${state.id}")
            EntityStateEvent.Detach.Post(this,state).call()
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
