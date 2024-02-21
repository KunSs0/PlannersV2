package com.gitee.planners.module.particle.shape

import com.gitee.planners.util.math.asVector
import org.ejml.simple.SimpleMatrix
import taboolib.common.util.Vector

class Point : ParticleShape() {

    var originVector = Vector(0, 0, 0)

    val origin = vector("origin", Vector(0, 0, 0)) {
        originVector = it.asVector()
    }

    override fun shape(t: Double): SimpleMatrix {
        return SimpleMatrix(1, 4, true, originVector.x, originVector.y, originVector.z, 1.0)
    }

}