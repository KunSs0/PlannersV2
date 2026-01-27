package com.gitee.planners.module.particle.animation

import com.gitee.planners.util.math.rotate
import org.ejml.simple.SimpleMatrix

class Rotate : ParticleAnimation() {

    var axis: Int = 0

    var degree: Double = 0.0
        set(value) {
            field = value
            radians = Math.toRadians(value)
        }

    private var radians: Double = 0.0

    override fun apply(input: SimpleMatrix, moment: Double): SimpleMatrix {
        return input.rotate(radians * moment, axis)
    }
}
