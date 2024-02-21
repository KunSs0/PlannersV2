package com.gitee.planners.module.particle.animation

import org.ejml.simple.SimpleMatrix
import taboolib.common5.cint

class SubShape : ParticleAnimated(transform = false) {

    private var last: Double = 0.0

    override fun apply(input: SimpleMatrix, moment: Double): SimpleMatrix {
        // Get a submatrix of the animation matrix
        val start = last
        last = moment
        val startRow: Int = (start * input.numRows()).cint
        val endRow: Int = (moment * input.numRows()).cint
        // Extract the submatrix
        return input.extractMatrix(startRow, endRow, 0, input.numCols())
    }

}
