package com.gitee.planners.module.particle.shape

import com.gitee.planners.api.common.entity.animated.AbstractAnimated
import com.gitee.planners.util.math.createIdentityMatrix
import org.ejml.simple.SimpleMatrix
import taboolib.common5.cdouble

/**
 * Shape of the particle
 *
 */
abstract class ParticleShape : AbstractAnimated() {

    val transformMatrix: SimpleMatrix = createIdentityMatrix()

    /**
     * Shape of the particle at the given moment
     * described as a function in 3d space
     *
     * @param t The moment. Between 0 and 1
     * @return The return has to be a nx4 matrix
     */
    abstract fun shape(t: Double): SimpleMatrix

    /**
     * Get the shape of the particle in a matrix
     *
     * @param numSamples The number of samples (not the number of particles)
     */
    fun getShape(numSamples: Int): SimpleMatrix {
        val step = 1.0 / (numSamples - 1).cdouble
        var t = 0.0
        var shape: SimpleMatrix? = null
        for (i in 0 until numSamples) {
            val vector = shape(t)

            assert(vector.numCols() == 4) { "The shape function has to return a nx4 matrix" }

            // Stack the vectors
            shape = if (shape == null) vector else shape.concatRows(vector)

            t += step
        }

        return shape!!.mult(transformMatrix)
    }

}
