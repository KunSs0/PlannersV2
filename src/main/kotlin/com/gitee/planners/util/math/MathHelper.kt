package com.gitee.planners.util.math

import com.gitee.planners.util.math.asVector
import org.bukkit.Location
import org.ejml.simple.SimpleMatrix
import taboolib.common.util.Vector
import taboolib.common5.cdouble


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
        // Array
        is Array<*> -> {
            assert(this.size == 3) {
                "Expected a 3d vector, but array has size ${this.size}"
            }
            try {
                after(this[0].cdouble, this[1].cdouble, this[2].cdouble)
            } catch (e: Exception) {
                error("Error while casting the array element to double!")
            }
        }

        is String -> {
            val split = split(" ")
            assert(split.size == 3) {
                "Expected a 3d vector, but string has size ${split.size}"
            }
            try {
                after(split[0].cdouble, split[1].cdouble, split[2].cdouble)
            } catch (e: Exception) {
                error("Error while casting the string element to double!")
            }
        }

        // EJML matrix
        is SimpleMatrix -> {
            assert((numCols() == 3 || numCols() == 4) && numRows() == 1) {
                "Expected 1x3 or 1x4 matrix but the matrix has dimensions ${numRows()}x${numCols()}"
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


