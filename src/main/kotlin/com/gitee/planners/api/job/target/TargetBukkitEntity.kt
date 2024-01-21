package com.gitee.planners.api.job.target

import com.gitee.planners.api.ProfileAPI.plannersProfile
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

class TargetBukkitEntity(val entity: Entity) : TargetEntity<Entity>, TargetCommandSender<Entity>,
    TargetContainerization {
    override fun getUniqueId(): UUID {
        return entity.uniqueId
    }

    override fun getEntityType(): EntityType {
        return entity.type
    }

    override fun getName(): String {
        return entity.name
    }

    override fun getInstance(): Entity {
        return entity
    }

    override fun getWorld(): String {
        return entity.world.name
    }

    override fun getBukkitWorld(): World? {
        return entity.world
    }

    override fun getBukkitLocation(): Location {
        return entity.location
    }

    override fun getX(): Double {
        return entity.location.x
    }

    override fun getY(): Double {
        return entity.location.y + entity.height / 2
    }

    override fun getZ(): Double {
        return entity.location.z
    }

    override fun sendMessage(message: String) {
        getInstance().sendMessage(message)
    }

    override fun dispatchCommand(command: String): Boolean {
        return Bukkit.dispatchCommand(getInstance(), command)
    }

    override fun getNearbyLivingEntities(vector: Vector): List<LivingEntity> {
        return getBukkitWorld()!!
            .getNearbyEntities(this.getInstance().location, vector.x, vector.y, vector.z)
            .filterIsInstance<LivingEntity>()
    }

    private fun getMetadataContainer(): MetadataContainer {
        return if (entity is Player) {
            entity.plannersProfile
        }
        // 通过代理实体获取元数据容器
        else {
            EntityMetadataManager.get(ProxyBukkitEntity(getInstance()))
        }
    }

    override fun getMetadata(id: String): Metadata? {
        return getMetadataContainer()[id]
    }

    override fun setMetadata(id: String, data: Metadata) {
        getMetadataContainer()[id] = data
    }

    override fun toString(): String {
        return "TargetBukkitEntity(entity=$entity)"
    }


}
