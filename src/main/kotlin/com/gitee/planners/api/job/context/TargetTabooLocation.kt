package com.gitee.planners.api.job.context

import org.bukkit.Bukkit
import org.bukkit.World
import taboolib.common.util.Location

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
}
