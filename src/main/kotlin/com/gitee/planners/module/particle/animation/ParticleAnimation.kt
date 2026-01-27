package com.gitee.planners.module.particle.animation

import org.ejml.simple.SimpleMatrix

/**
 * An animation is a transformation that can be applied to a shape at a given moment.
 *
 * @property transform  If true, the animation will transform the shape.
 *                      If false, the animation will transform the shape's vertices.

 */
abstract class ParticleAnimation(val transform: Boolean = false) {

    var start: Int = 0

    var end: Int = 1

    var stay: Boolean = true

    /**
     * Play the animation at the given moment. moment is a value between 0 and 1.
     */
    abstract fun apply(input: SimpleMatrix, moment: Double): SimpleMatrix

}
