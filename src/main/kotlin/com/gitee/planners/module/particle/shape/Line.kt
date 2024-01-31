package com.gitee.planners.module.particle.shape

import org.ejml.simple.SimpleMatrix
import taboolib.common.util.Vector

class Line : Shape() {

    private val pointMatrix = SimpleMatrix(1, 4, true, 0.0, 0.0, 0.0, 1.0)

    private val directionMatrix = SimpleMatrix(1, 4, true, 1.0, 0.0, 0.0, 0.0)

    val point = vector("point", Vector(0, 0, 0)) {
        pointMatrix[0, 0] = it.x
        pointMatrix[0, 1] = it.y
        pointMatrix[0, 2] = it.z
    }

    val direction = vector("direction", Vector(1, 0, 0)) {
        directionMatrix[0, 0] = it.x
        directionMatrix[0, 1] = it.y
        directionMatrix[0, 2] = it.z
    }

    override fun shape(t: Double): SimpleMatrix {
        return pointMatrix.plus(directionMatrix.scale(t))
    }
}