package com.gitee.planners.util

import org.bukkit.Location
import org.ejml.simple.SimpleMatrix
import taboolib.common.util.Vector
import kotlin.math.cos
import kotlin.math.sin

typealias MatrixTransform = (SimpleMatrix) -> SimpleMatrix


/**
 * Safe cast a data to a vector and return a copy
 *
 * @param after The function to call after the cast and return the vector
 * @throws IllegalStateException If the class is not a vector or of wrong dimensions
 */
inline fun <reified T : Any> Any.asVector(after: (x: Double, y: Double, z: Double) -> T): T {
    return when (this) {
        // Normal vector
        is Vector -> after(x, y, z)
        // Bukkit location
        is Location -> after(x, y, z)
        // EJML matrix
        is SimpleMatrix -> {
            assert((numCols == 3 || numCols == 4) && numRows == 1) {
                "Expected 1x3 or 1x4 matrix but the matrix has dimensions ${numRows}x${numCols}"
            }
            after(this[0, 0], this[0, 1], this[0, 2])
        }

        else -> error("The class is of type ${this::class.java} and is not a vector")
    }
}

/**
 * Safe cast a data to a vector and return a copy
 */
fun Any.asVector(): Vector {
    return asVector { x, y, z -> Vector(x, y, z) }
}

/**
 * Rotate the matrix around the given axis
 *
 * @param angle The angle in radians
 * @param axis The axis to rotate around. 0 is x, 1 is y, 2 is z
 */
fun rotateMatrix(angle: Double, axis: Int): MatrixTransform {
    val cos = cos(angle)
    val sin = sin(angle)
    val (x, y, z) = when (axis) {
        0 -> Triple(1, 2, 3)
        1 -> Triple(2, 3, 1)
        2 -> Triple(3, 1, 2)
        else -> throw IllegalArgumentException("Axis has to be 0, 1 or 2")
    }
    val rotateMatrix4f = SimpleMatrix(doubleArrayOf(
            if (x == 1) 1.0 else cos, if (y == 1) -sin else 0.0, if (z == 1) sin else 0.0, 0.0,
            if (x == 2) 0.0 else sin, if (y == 2) 1.0 else cos, if (z == 2) -sin else 0.0, 0.0,
            if (x == 3) 0.0 else -sin, if (y == 3) sin else 0.0, if (z == 3) 1.0 else cos, 0.0,
            0.0, 0.0, 0.0, 1.0
    ))
    return { matrix ->
        rotateMatrix4f.mult(matrix)
    }
}

/**
 * Scale the matrix
 *
 * @param x The scale factor in the x direction
 * @param y The scale factor in the y direction
 * @param z The scale factor in the z direction

 */
fun scaleMatrix(x: Double, y: Double, z: Double): MatrixTransform {
    val scaleMatrix4f = SimpleMatrix(doubleArrayOf(
            x, 0.0, 0.0, 0.0,
            0.0, y, 0.0, 0.0,
            0.0, 0.0, z, 0.0,
            0.0, 0.0, 0.0, 1.0
    ))
    return { matrix ->
        scaleMatrix4f.mult(matrix)
    }
}

/**
 * Translate the matrix
 *
 * @param x The translation in the x direction
 * @param y The translation in the y direction
 * @param z The translation in the z direction
 */
fun translateMatrix(x: Double, y: Double, z: Double): MatrixTransform {
    val translateMatrix4f = SimpleMatrix(doubleArrayOf(
            1.0, 0.0, 0.0, x,
            0.0, 1.0, 0.0, y,
            0.0, 0.0, 1.0, z,
            0.0, 0.0, 0.0, 1.0
    ))
    return { matrix ->
        translateMatrix4f.mult(matrix)
    }
}

/**
 * Apply the identity matrix
 */
fun identityMatrix(): MatrixTransform {
    val identityMatrix4f = SimpleMatrix(doubleArrayOf(
            1.0, 0.0, 0.0, 0.0,
            0.0, 1.0, 0.0, 0.0,
            0.0, 0.0, 1.0, 0.0,
            0.0, 0.0, 0.0, 1.0
    ))
    return { matrix ->
        identityMatrix4f.mult(matrix)
    }
}