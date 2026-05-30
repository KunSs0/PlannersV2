package com.gitee.planners.api.common.util

import org.bukkit.entity.Player
import org.bukkit.util.Vector

class PathTrace(val origin: Vector, val direction: Vector) {

    constructor(player: Player) : this(player.eyeLocation.toVector(), player.eyeLocation.direction)

    fun traces(distance: Double, accuracy: Double): Set<Vector> {
        val vectors = mutableSetOf<Vector>()
        var process = 0.0
        while (process <= distance) {
            vectors.add(distance(process))
            process += accuracy
        }
        return vectors
    }
    fun distance(distance: Double): Vector {
        return origin.clone().add(direction.clone().multiply(distance))
    }
}
