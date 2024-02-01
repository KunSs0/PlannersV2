package com.gitee.planners.util.math

import com.gitee.planners.api.common.metadata.Metadata
import org.ejml.simple.SimpleMatrix
import kotlin.math.cos
import kotlin.math.sin


fun Metadata.toVector3f(): SimpleMatrix {
    return any().asVector { x, y, z ->
        SimpleMatrix(1, 3, true, doubleArrayOf(x, y, z))
    }
}

fun Metadata.toVector4f(): SimpleMatrix {
    return any().asVector { x, y, z ->
        SimpleMatrix(1, 4, true, doubleArrayOf(x, y, z, 1.0))
    }
}

fun Metadata.toVector4d(): SimpleMatrix {
    return any().asVector { x, y, z ->
        SimpleMatrix(1, 4, true, doubleArrayOf(x, y, z, 0.0))
    }
}

fun createZeroMatrix(): SimpleMatrix {
    return SimpleMatrix(
        arrayOf(
            doubleArrayOf(0.0, 0.0, 0.0, 0.0),
            doubleArrayOf(0.0, 0.0, 0.0, 0.0),
            doubleArrayOf(0.0, 0.0, 0.0, 0.0),
            doubleArrayOf(0.0, 0.0, 0.0, 0.0),
        )
    )
}

/**
 * Rotate the matrix around the given axis
 *
 * @param angle The angle in radians
 * @param axis The axis to rotate around. 0 is x, 1 is y, 2 is z
 */
fun SimpleMatrix.rotation(angle: Double, axis: Int): SimpleMatrix {
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

/**
 * Scale the matrix
 *
 * @param x The scale factor in the x direction
 * @param y The scale factor in the y direction
 * @param z The scale factor in the z direction

 */
fun SimpleMatrix.scale(x: Double, y: Double, z: Double): SimpleMatrix {
    return this.mult(
        SimpleMatrix(
            arrayOf(
                doubleArrayOf(x, 0.0, 0.0, 0.0),
                doubleArrayOf(0.0, y, 0.0, 0.0),
                doubleArrayOf(0.0, 0.0, z, 0.0),
                doubleArrayOf(0.0, 0.0, 0.0, 1.0)
            )
        )
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
    return SimpleMatrix(
        arrayOf(
            doubleArrayOf(1.0, 0.0, 0.0, 0.0),
            doubleArrayOf(0.0, 1.0, 0.0, 0.0),
            doubleArrayOf(0.0, 0.0, 1.0, 0.0),
            doubleArrayOf(0.0, 0.0, 0.0, 1.0)
        )
    )

}
