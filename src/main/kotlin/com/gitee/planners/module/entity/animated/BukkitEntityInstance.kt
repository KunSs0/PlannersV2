package com.gitee.planners.module.entity.animated

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.api.job.target.TargetCommandSender
import com.gitee.planners.api.job.target.TargetContainerization
import com.gitee.planners.api.job.target.TargetEntity
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import taboolib.common.util.Vector
import taboolib.module.nms.getI18nName
import java.util.*

/**
 * Bukkit 实体实例 Animated
 */
class BukkitEntityInstance(override var instance: Entity) : AbstractBukkitEntityAnimated<Entity>(),TargetEntity<Entity>,TargetCommandSender<Entity>,TargetContainerization {

    private val proxy = TargetBukkitEntity(instance)

    override fun getUniqueId(): UUID {
        return proxy.getUniqueId()
    }

    override fun getEntityType(): EntityType {
        return proxy.getEntityType()
    }

    override fun getBukkitEyeLocation(): Location {
        return proxy.getBukkitEyeLocation()
    }

    override fun getWorld(): String {
        return proxy.getWorld()
    }

    override fun getBukkitWorld(): World? {
        return proxy.getBukkitWorld()
    }

    override fun getBukkitLocation(): Location {
        return proxy.getBukkitLocation()
    }

    override fun getX(): Double {
        return proxy.getX()
    }

    override fun getY(): Double {
        return proxy.getY()
    }

    override fun getZ(): Double {
        return proxy.getZ()
    }

    override fun add(x: Double, y: Double, z: Double) {

    }

    override fun getNearbyLivingEntities(vector: Vector): List<LivingEntity> {
        return proxy.getNearbyLivingEntities(vector)
    }

    override fun sendMessage(message: String) {
        proxy.sendMessage(message)
    }

    override fun dispatchCommand(command: String): Boolean {
        return proxy.dispatchCommand(command)
    }

    override fun getName(): String {
        return proxy.getName()
    }

    override fun getMetadata(id: String): Metadata? {
        return proxy.getMetadata(id)
    }

    override fun setMetadata(id: String, data: Metadata) {
        proxy.setMetadata(id, data)
    }


}
