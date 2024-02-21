package com.gitee.planners.module.particle

import com.gitee.planners.module.particle.animation.ParticleAnimated
import com.gitee.planners.util.math.createIdentityMatrix
import org.ejml.simple.SimpleMatrix

class BukkitParticleFramework(animations: MutableList<ParticleAnimated>, bakedShape: SimpleMatrix, var tick: Int, val duration: Int, val backward: Boolean = false, val spawner: (x: Double, y: Double, z: Double) -> Unit) {

    val animations: MutableList<ParticleAnimated>

    var bakedShape: SimpleMatrix

    var finished: Boolean = false

    init {
        // Set the baked shape
        this.bakedShape = bakedShape.copy()!!
        // Set the animations
        this.animations = animations.toMutableList()
    }

    private fun moment(animation: ParticleAnimated): Double {
        return if (!backward) {
            (tick - animation.start.asInt()) / (animation.end.asInt() - animation.start.asInt()).toDouble()
        } else {
            (animation.end.asInt() - tick) / (animation.end.asInt() - animation.start.asInt()).toDouble()
        }
    }

    /**
     * Animate the next frame
     *
     * @return true if the animation is still playing
     */
    fun nextFrame(): Boolean {
        if (tick > duration) {
            tick = 0
            finished = true
            return false // no more frames
        }

        // Get the transformation matrix
        var transformMatrix: SimpleMatrix = createIdentityMatrix()

        val nonTransformAnimations = mutableListOf<ParticleAnimated>()

        val animationIterator = animations.iterator()
        while (animationIterator.hasNext()) {
            val anim = animationIterator.next()
            // Apply the transform animations to the matrix
            if (anim.start.asInt() < tick && tick <= anim.end.asInt()) {
                if (anim.transform)
                    transformMatrix = anim.apply(transformMatrix, moment(anim))
                else // Apply the non-transform animations later
                    nonTransformAnimations.add(anim)
            } else if (tick > anim.end.asInt()) {
                if (anim.stay.asBoolean()) {
                    bakedShape = anim.apply(bakedShape, 1.0)
                }
                // Remove the animation if it is finished
                animationIterator.remove()
            }
        }

        // Get the matrix for display
        var shape = bakedShape.copy()!!
        for (anim in nonTransformAnimations) { // Non-transform animations are directly applied to the shape
            shape = anim.apply(shape, moment(anim))
        }
        shape = shape.mult(transformMatrix)

        // Display the frame
        for (i in 0 until shape.numRows()) {
            val vector = shape.extractVector(true, i) ?: break
            spawner(vector.get(0), vector.get(1), vector.get(2))
        }

        tick += 1
        return true
    }
}
