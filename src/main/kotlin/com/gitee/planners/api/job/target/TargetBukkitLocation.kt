package com.gitee.planners.api.job.target

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.LivingEntity
import taboolib.common.util.Vector

open class TargetBukkitLocation(override val instance: Location) : TargetLocation<Location> {

    override fun getWorld(): String {
        return instance.world!!.name
    }

    override fun getBukkitWorld(): World? {
        return instance.world
    }

    override fun getX(): Double {
        return instance.x
    }

    override fun getY(): Double {
        return instance.y
    }

    override fun getZ(): Double {
        return instance.z
    }

    override fun getBukkitLocation(): Location {
        return instance
    }

    override fun getNearbyLivingEntities(vector: Vector): List<LivingEntity> {
        return getBukkitWorld()!!
            .getNearbyEntities(this.instance, vector.x, vector.y, vector.z)
            .filterIsInstance<LivingEntity>()
    }

    override fun add(x: Double, y: Double, z: Double) {
        this.instance.add(x, y, z)
    }

    override fun toString(): String {
        return "TargetBukkitLocation(instance=$instance)"
    }


}
