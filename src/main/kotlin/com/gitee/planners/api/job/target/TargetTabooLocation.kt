package com.gitee.planners.api.job.target

import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.LivingEntity
import taboolib.common.util.Location
import taboolib.common.util.Vector
import taboolib.platform.util.toBukkitLocation

class TargetTabooLocation(val location: Location) : TargetLocation<Location> {
    override fun getWorld(): String {
        return location.world!!
    }

    override fun getBukkitWorld(): World? {
        return Bukkit.getWorld(getWorld()) ?: error("Couldn't get world from ${getWorld()}'")
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
            .getNearbyEntities(getInstance().toBukkitLocation(), vector.x, vector.y, vector.z)
            .filterIsInstance<LivingEntity>()
    }

}
