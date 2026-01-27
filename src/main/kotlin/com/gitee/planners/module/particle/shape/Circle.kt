package com.gitee.planners.module.particle.shape

import org.ejml.simple.SimpleMatrix
import taboolib.common.util.Vector
import kotlin.math.cos
import kotlin.math.sin

class Circle : ParticleShape() {

    private val centerMatrix = SimpleMatrix(1, 4, true, doubleArrayOf(0.0, 0.0, 0.0, 1.0))

    var center: Vector = Vector(0, 0, 0)
        set(value) {
            field = value
            centerMatrix[0, 0] = value.x
            centerMatrix[0, 1] = value.y
            centerMatrix[0, 2] = value.z
        }

    var radius: Double = 1.0

    override fun shape(t: Double): SimpleMatrix {
        val radians = 2.0 * Math.PI * t
        val x = cos(radians)
        val y = sin(radians)
        return centerMatrix.plus(SimpleMatrix(1, 4, true, doubleArrayOf(x * radius, y * radius, 0.0, 0.0)))
    }

}
