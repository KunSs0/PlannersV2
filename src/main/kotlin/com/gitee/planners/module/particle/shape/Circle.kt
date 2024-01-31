package com.gitee.planners.module.particle.shape

import org.ejml.simple.SimpleMatrix
import taboolib.common.util.Vector
import kotlin.math.cos
import kotlin.math.sin

class Circle : Shape() {

    private val centerMatrix = SimpleMatrix(1, 4, true, 0.0, 0.0, 0.0, 1.0)

    val center = vector("center", Vector(0, 0, 0)) {
        centerMatrix[0, 0] = it.x
        centerMatrix[0, 1] = it.y
        centerMatrix[0, 2] = it.z
    }

    val radius = double("radius", 1.0) { }

    override fun shape(t: Double): SimpleMatrix {
        // Here the t is the theta
        val radians = 2.0 * Math.PI * t
        val x = cos(radians)
        val y = sin(radians)
        val rad = radius.asDouble()
        return centerMatrix.plus(SimpleMatrix(1, 4, true, x * rad, y * rad, 0.0, 0.0))
    }

}