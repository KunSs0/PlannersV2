package com.gitee.planners.api.common

import taboolib.common5.cdouble
import taboolib.common5.cfloat

interface WorkableTransform {

    abstract class TextToNumber<T : Number>(val source: String, val prefix: String) : WorkableTransform {

        val isSpecial = source.startsWith(prefix)

        abstract fun add(value: T, offset: T): T

        abstract fun normalize(value: String): T

        fun build(offset: () -> T): T {
            return if (isSpecial) add(normalize(source.substring(1)), offset()) else normalize(source)
        }
    }

    companion object {

        fun buildFloat(source: String, prefix: String = "~", offset: () -> Float): Float {
            val workable = object : TextToNumber<Float>(source, prefix) {
                override fun add(value: Float, offset: Float): Float {
                    return value + offset
                }

                override fun normalize(value: String): Float {
                    return value.cfloat
                }

            }
            return workable.build(offset)
        }

    }

}
