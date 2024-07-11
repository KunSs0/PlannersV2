package com.gitee.planners.api.job.target

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.entity.ProxyBukkitEntity
import com.gitee.planners.api.common.metadata.EntityMetadataManager
import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.common.metadata.MetadataContainer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import taboolib.common.util.Vector
import java.util.*

class TargetBukkitEntity(override val instance: Entity) : TargetEntity<Entity>, TargetCommandSender<Entity>, TargetContainerization {


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
            EntityMetadataManager.get(ProxyBukkitEntity(instance))
        }
    }

    override fun getMetadata(id: String): Metadata? {
        return getMetadataContainer()[id]
    }

    override fun setMetadata(id: String, data: Metadata) {
        getMetadataContainer()[id] = data
    }

    override fun toString(): String {
        return "TargetBukkitEntity(instance=$instance)"
    }


}
