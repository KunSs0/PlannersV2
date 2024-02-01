package com.gitee.planners.module.particle.animation

import com.gitee.planners.api.common.entity.animated.AbstractAnimated
import com.gitee.planners.util.math.MatrixTransform
import org.ejml.simple.SimpleMatrix

/**
 * An animation is a transformation that can be applied to a shape at a given moment.
 *
 * @property transform  If true, the animation will transform the shape.
 *                      If false, the animation will transform the shape's vertices.

 */
abstract class Animation(val transform: Boolean = false) : AbstractAnimated() {

    val start = strictInt("start", 0) { }

    val end = strictInt("end", 1) { }

    /**
     * Play the animation at the given moment. moment is a value between 0 and 1.
     */
    abstract fun transform(moment: Double): SimpleMatrix

    open fun play(moment: Double): MatrixTransform {
        return { m -> m.mult(transform(moment)) }
    }

}