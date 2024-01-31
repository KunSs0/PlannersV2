package com.gitee.planners.module.particle.animation

import com.gitee.planners.util.rotateMatrix
import taboolib.common5.cint

class AnimationRotate : Animation() {

    val axis = createBaked("axis", 0,
            parser = {
                when {
                    (this is Int) -> this.cint.coerceAtMost(2).coerceAtLeast(0)
                    (this is String) -> when {
                        this.equals("x", true) -> 0
                        this.equals("y", true) -> 1
                        this.equals("z", true) -> 2
                        else -> 0
                    }

                    else -> 0
                }
            }) { }

    val degree = double("degree", 0.0) {
        radians = Math.toRadians(it)
    }

    private var radians: Double = 0.0

    override fun play(moment: Double) = rotateMatrix(radians, axis.asInt())
}