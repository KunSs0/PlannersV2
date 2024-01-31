package com.gitee.planners.module.particle.shape

import org.ejml.simple.SimpleMatrix

class DynamicShape(val func: (t: Double) -> SimpleMatrix) : Shape() {

    override fun shape(t: Double): SimpleMatrix {
        return func(t)
    }
}