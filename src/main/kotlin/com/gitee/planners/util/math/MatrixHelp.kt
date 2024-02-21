package com.gitee.planners.util.math

import org.ejml.simple.SimpleMatrix
import taboolib.common.util.Vector
import kotlin.math.cos
import kotlin.math.sin


fun Vector.asSimpleMatrix(): SimpleMatrix {
    return SimpleMatrix(
            arrayOf(
                    doubleArrayOf(x, y, z, 1.0)
            )
    )
}

fun Vector.asDirectionSimpleMatrix(): SimpleMatrix {
    return SimpleMatrix(
            arrayOf(
                    doubleArrayOf(x, y, z, 0.0)
            )
    )
}

fun createZeroMatrix(): SimpleMatrix {
    return SimpleMatrix.filled(4, 4, 0.0)
}

/**
 * Rotate the matrix around the given axis
 *
 * @param angle The angle in radians
 * @param axis The axis to rotate around. 0 is x, 1 is y, 2 is z
 */
fun SimpleMatrix.rotate(angle: Double, axis: Int): SimpleMatrix {
    val cos = cos(angle)
    val sin = sin(angle)
    val (x, y, z) = when (axis) {
        0 -> Triple(1, 2, 3)
        1 -> Triple(2, 3, 1)
        2 -> Triple(3, 1, 2)
        else -> throw IllegalArgumentException("Axis has to be 0, 1 or 2")
    }
    return this.mult(
            SimpleMatrix(
                    arrayOf(
                            doubleArrayOf(if (x == 1) 1.0 else cos, if (y == 1) -sin else 0.0, if (z == 1) sin else 0.0, 0.0),
                            doubleArrayOf(if (x == 2) 0.0 else sin, if (y == 2) 1.0 else cos, if (z == 2) -sin else 0.0, 0.0),
                            doubleArrayOf(if (x == 3) 0.0 else -sin, if (y == 3) sin else 0.0, if (z == 3) 1.0 else cos, 0.0),
                            doubleArrayOf(0.0, 0.0, 0.0, 1.0)
                    )
            )
    )
}

fun SimpleMatrix.rotate(angle: Double, axis: Vector): SimpleMatrix {
    val cos = cos(angle)
    val sin = sin(angle)
    val x = axis.x
    val y = axis.y
    val z = axis.z
    return this.mult(
            SimpleMatrix(
                    arrayOf(
                            doubleArrayOf(
                                    cos + x * x * (1 - cos),
                                    x * y * (1 - cos) - z * sin,
                                    x * z * (1 - cos) + y * sin,
                                    0.0
                            ),
                            doubleArrayOf(
                                    y * x * (1 - cos) + z * sin,
                                    cos + y * y * (1 - cos),
                                    y * z * (1 - cos) - x * sin,
                                    0.0
                            ),
                            doubleArrayOf(
                                    z * x * (1 - cos) - y * sin,
                                    z * y * (1 - cos) + x * sin,
                                    cos + z * z * (1 - cos),
                                    0.0
                            ),
                            doubleArrayOf(0.0, 0.0, 0.0, 1.0)
                    )
            )
    )
}

/**
 * Scale the matrix
 *
 * @param x The scale factor in the x direction
 * @param y The scale factor in the y direction
 * @param z The scale factor in the z direction

 */
fun SimpleMatrix.scale(x: Double, y: Double, z: Double): SimpleMatrix {
    return this.mult(
            SimpleMatrix.diag(x, y, z, 1.0)
    )
}

/**
 * Translate the matrix
 *
 * @param x The translation in the x direction
 * @param y The translation in the y direction
 * @param z The translation in the z direction
 */
fun SimpleMatrix.translate(x: Double, y: Double, z: Double): SimpleMatrix {
    return this.mult(
            SimpleMatrix(
                    arrayOf(
                            doubleArrayOf(1.0, 0.0, 0.0, x),
                            doubleArrayOf(0.0, 1.0, 0.0, y),
                            doubleArrayOf(0.0, 0.0, 1.0, z),
                            doubleArrayOf(0.0, 0.0, 0.0, 1.0)
                    )
            )
    )
}

/**
 * Apply the identity matrix
 */
fun createIdentityMatrix(): SimpleMatrix {
    return SimpleMatrix.identity(4)
}
