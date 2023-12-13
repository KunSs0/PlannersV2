package com.gitee.planners.api.job.target

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.LivingEntity
import taboolib.common.util.Vector

open class TargetBukkitLocation(val location: Location) : TargetLocation<Location> {

    override fun getWorld(): String {
        return location.world!!.name
    }

    override fun getBukkitWorld(): World? {
        return location.world
    }

    override fun getX(): Double {
        return location.x
    }

    override fun getY(): Double {
        return location.y
    }

    override fun getZ(): Double {
        return location.z
    }

    override fun getInstance(): Location {
        return location
    }

    override fun getNearbyLivingEntities(vector: Vector): List<LivingEntity> {
        return getBukkitWorld()!!
            .getNearbyEntities(this.location, vector.x, vector.y, vector.z)
            .filterIsInstance<LivingEntity>()
    }


}
