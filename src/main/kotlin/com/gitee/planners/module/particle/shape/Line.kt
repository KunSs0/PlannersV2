package com.gitee.planners.module.particle.shape

import org.ejml.simple.SimpleMatrix
import taboolib.common.util.Vector

class Line : ParticleShape() {

    private val pointMatrix = SimpleMatrix(1, 4, true, doubleArrayOf(0.0, 0.0, 0.0, 1.0))

    private val directionMatrix = SimpleMatrix(1, 4, true, doubleArrayOf(1.0, 0.0, 0.0, 0.0))

    var origin: Vector = Vector(0, 0, 0)
        set(value) {
            field = value
            pointMatrix[0, 0] = value.x
            pointMatrix[0, 1] = value.y
            pointMatrix[0, 2] = value.z
        }

    var direction: Vector = Vector(1, 1, 1)
        set(value) {
            field = value
            directionMatrix[0, 0] = value.x
            directionMatrix[0, 1] = value.y
            directionMatrix[0, 2] = value.z
        }

    override fun shape(t: Double): SimpleMatrix {
        return pointMatrix.plus(directionMatrix.scale(t))
    }
}
