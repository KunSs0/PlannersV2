package com.gitee.planners.api.job.target

import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.LivingEntity
import taboolib.common.util.Location
import taboolib.common.util.Vector
import taboolib.common5.cdouble
import taboolib.platform.util.toBukkitLocation

class TargetTabooLocation(val location: Location) : TargetLocation<Location> {

    override val instance = location

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

    override fun getBukkitLocation(): org.bukkit.Location {
        return location.toBukkitLocation()
    }

    override fun getZ(): Double {
        return location.z
    }

    override fun getYaw(): Double {
        return location.yaw.cdouble
    }

    override fun getPitch(): Double {
        return location.pitch.cdouble
    }

    override fun add(x: Double, y: Double, z: Double) {
        location.add(x,y,z)
    }


    override fun getNearbyLivingEntities(vector: Vector): List<LivingEntity> {
        return getBukkitWorld()!!
            .getNearbyEntities(instance.toBukkitLocation(), vector.x, vector.y, vector.z)
            .filterIsInstance<LivingEntity>()
    }

    override fun toString(): String {
        return "TargetTabooLocation(location=$location)"
    }


}
