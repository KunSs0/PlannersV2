package com.gitee.planners.module.particle

import com.gitee.planners.module.particle.animation.ParticleAnimation
import com.gitee.planners.util.math.createIdentityMatrix
import org.ejml.simple.SimpleMatrix

class BukkitParticleFramework(
    animations: MutableList<ParticleAnimation>,
    bakedShape: SimpleMatrix,
    var tick: Int,
    val duration: Int,
    val backward: Boolean = false,
    val spawner: (x: Double, y: Double, z: Double) -> Unit
) {

    val animations: MutableList<ParticleAnimation>

    var bakedShape: SimpleMatrix

    var finished: Boolean = false

    init {
        this.bakedShape = bakedShape.copy()!!
        this.animations = animations.toMutableList()
    }

    private fun moment(animation: ParticleAnimation): Double {
        return if (!backward) {
            (tick - animation.start) / (animation.end - animation.start).toDouble()
        } else {
            (animation.end - tick) / (animation.end - animation.start).toDouble()
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
            return false
        }

        var transformMatrix: SimpleMatrix = createIdentityMatrix()
        val nonTransformAnimations = mutableListOf<ParticleAnimation>()

        val animationIterator = animations.iterator()
        while (animationIterator.hasNext()) {
            val anim = animationIterator.next()
            if (anim.start < tick && tick <= anim.end) {
                if (anim.transform)
                    transformMatrix = anim.apply(transformMatrix, moment(anim))
                else
                    nonTransformAnimations.add(anim)
            } else if (tick > anim.end) {
                if (anim.stay) {
                    bakedShape = anim.apply(bakedShape, 1.0)
                }
                animationIterator.remove()
            }
        }

        var shape = bakedShape.copy()!!
        for (anim in nonTransformAnimations) {
            shape = anim.apply(shape, moment(anim))
        }
        shape = shape.mult(transformMatrix)

        for (i in 0 until shape.numRows()) {
            val vector = shape.extractVector(true, i) ?: break
            spawner(vector.get(0), vector.get(1), vector.get(2))
        }

        tick += 1
        return true
    }
}
