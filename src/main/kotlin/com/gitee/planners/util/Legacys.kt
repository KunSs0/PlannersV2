package com.gitee.planners.util

import org.bukkit.util.Vector
import kotlin.math.cos
import kotlin.math.sin

fun rotateAroundX(vector: Vector, angle: Double): Vector {
    if (angle == 0.0) return vector
    val angleCos = cos(Math.toRadians(angle))
    val angleSin = sin(Math.toRadians(angle))
    val y: Double = angleCos * vector.y - angleSin * vector.z
    val z: Double = angleSin * vector.y + angleCos * vector.z
    return vector.setY(y).setZ(z)
}

fun rotateAroundY(vector: Vector, angle: Double): Vector {
    if (angle == 0.0) return vector
    val angleCos = cos(Math.toRadians(angle))
    val angleSin = sin(Math.toRadians(angle))
    val x: Double = angleCos * vector.x + angleSin * vector.z
    val z: Double = -angleSin * vector.x + angleCos * vector.z
    return vector.setX(x).setZ(z)
}

fun rotateAroundZ(vector: Vector, angle: Double): Vector {
    if (angle == 0.0) return vector
    val angleCos = cos(Math.toRadians(angle))
    val angleSin = sin(Math.toRadians(angle))
    val x: Double = angleCos * vector.x - angleSin * vector.y
    val y: Double = angleSin * vector.x + angleCos * vector.y
    return vector.setX(x).setY(y)
}
