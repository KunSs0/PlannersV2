package com.gitee.planners.api.common.util

import org.bukkit.entity.Player
import org.bukkit.util.Vector

class PathTrace(val origin: Vector, val direction: Vector) {

    constructor(player: Player) : this(player.eyeLocation.toVector(), player.eyeLocation.direction)

    fun traces(distance: Double, accuracy: Double): Set<Vector> {
        return mutableSetOf<Vector>().let {
            var process = 0.0
            while (process <= distance) {
                it.add(distance(process))
                process += accuracy
            }
            it
        }
    }
    fun distance(distance: Double): Vector {
        return origin.clone().add(direction.clone().multiply(distance))
    }
}
