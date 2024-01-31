package com.gitee.planners.module.particle.animation

import com.gitee.planners.util.MatrixTransform
import taboolib.common5.cint

class AnimationWindow : Animation() {

    private var last: Double = 0.0

    override fun play(moment: Double): MatrixTransform {
        // Get a submatrix of the animation matrix
        val start = last
        last = moment
        return { shape ->
            val startRow: Int = (start * shape.numRows).cint
            val endRow: Int = (moment * shape.numRows).cint
            // Extract the submatrix
            shape.extractMatrix(startRow, endRow, 0, shape.numCols)
        }
    }

}