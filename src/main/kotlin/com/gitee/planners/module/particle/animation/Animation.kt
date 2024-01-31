package com.gitee.planners.module.particle.animation

import com.gitee.planners.api.common.entity.animated.AbstractAnimated
import com.gitee.planners.util.MatrixTransform

abstract class Animation : AbstractAnimated() {

    val start = strictInt("start", 0) { }

    val end = strictInt("end", 1) { }

    /**
     * Play the animation at the given moment. moment is a value between 0 and 1.
     */
    abstract fun play(moment: Double): MatrixTransform

}