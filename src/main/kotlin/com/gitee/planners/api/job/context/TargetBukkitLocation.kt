package com.gitee.planners.api.job.context

import org.bukkit.Location
import org.bukkit.World

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


}
