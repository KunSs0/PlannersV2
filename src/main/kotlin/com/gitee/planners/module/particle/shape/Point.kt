package com.gitee.planners.module.particle.shape

import org.ejml.simple.SimpleMatrix
import taboolib.common.util.Vector

class Point : ParticleShape() {

    var origin: Vector = Vector(0, 0, 0)

    override fun shape(t: Double): SimpleMatrix {
        return SimpleMatrix(1, 4, true, doubleArrayOf(origin.x, origin.y, origin.z, 1.0))
    }

}
