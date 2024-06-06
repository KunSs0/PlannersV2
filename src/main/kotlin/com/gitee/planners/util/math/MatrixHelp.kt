package com.gitee.planners.util.math

import org.ejml.simple.SimpleMatrix
import taboolib.common.util.Vector
import kotlin.math.cos
import kotlin.math.sin


fun Vector.asSimpleMatrix(): SimpleMatrix {
    return SimpleMatrix(arrayOf(doubleArrayOf(x, y, z, 1.0)))
}

fun Vector.asDirectionSimpleMatrix(): SimpleMatrix {
    return SimpleMatrix(arrayOf(doubleArrayOf(x, y, z, 0.0)))
}

fun createZeroMatrix(): SimpleMatrix {
    val map = (0 until 4).map { doubleArrayOf(0.0, 0.0, 0.0, 0.0) }.toTypedArray()
    return SimpleMatrix(map)
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
    val row1 = doubleArrayOf(
        cos + x * x * (1 - cos),
        x * y * (1 - cos) - z * sin,
        x * z * (1 - cos) + y * sin,
        0.0
    )
    val row2 = doubleArrayOf(
        y * x * (1 - cos) + z * sin,
        cos + y * y * (1 - cos),
        y * z * (1 - cos) - x * sin,
        0.0
    )
    val row3 = doubleArrayOf(
        z * x * (1 - cos) - y * sin,
        z * y * (1 - cos) + x * sin,
        cos + z * z * (1 - cos),
        0.0
    )
    return this.mult(SimpleMatrix(arrayOf(row1,row2,row3, doubleArrayOf(0.0, 0.0, 0.0, 1.0))))
}

/**
 * Scale the matrix
 *
 * @param x The scale factor in the x direction
 * @param y The scale factor in the y direction
 * @param z The scale factor in the z direction

 */
fun SimpleMatrix.scale(x: Double, y: Double, z: Double): SimpleMatrix {
    return this.mult(SimpleMatrix.diag(x, y, z, 1.0))
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

fun Any.asTransformMatrix(): SimpleMatrix {
    return if (this is SimpleMatrix) {
        assert(numCols() == 4 && numRows() == 4) {
            "Expected 4x4 matrix but the matrix has dimensions ${numRows()}x${numCols()}"
        }
        this
    } else {
        error("The class is of type ${this::class.java} and is not a matrix")
    }
}

fun Any.asScaleMatrix(): SimpleMatrix {
    val vector = this.asVector()
    return createIdentityMatrix().scale(vector.x, vector.y, vector.z)
}

