package com.gitee.planners.api.common.metadata

import com.gitee.planners.util.math.asVector
import com.google.gson.JsonElement
import org.ejml.simple.SimpleMatrix
import taboolib.common.util.Vector
import taboolib.common5.*

class MetadataTypeToken {

    class Void : TypeToken(String::class.java, Any(), -1) {

        override fun isTimeout(): Boolean {
            return true
        }

    }

    open class TypeToken(override val clazz: Class<*>, var any: Any, override val stopTime: Long) : Metadata {

        override fun isTimeout(): Boolean {
            return stopTime != -1L && System.currentTimeMillis() > stopTime
        }

        override fun asBoolean(): Boolean {
            return any.cbool
        }

        override fun asDouble(): Double {
            return any.cdouble
        }

        override fun asFloat(): Float {
            return any.cfloat
        }

        override fun any(): Any {
            return any
        }

        override fun asInt(): Int {
            return any.cint
        }

        override fun asLong(): Long {
            return any.clong
        }

        override fun asString(): String {
            return any.toString()
        }

        override fun asVector(): Vector {
            return any.asVector()
        }

        override fun increase(value: Any) {
            this.any = when (clazz) {
                java.lang.Integer::class.java -> asInt() + value.cint
                java.lang.Double::class.java -> asDouble() + value.cdouble
                java.lang.Float::class.java -> asFloat() + value.cfloat
                java.lang.Long::class.java -> asLong() + value.clong

                else -> error("")
            }
            println("increase ${this.any}")
        }

    }

    class SerializableS : Metadata.Serializable<String> {

        override fun type(): Class<String> {
            return String::class.java
        }

        override fun decode(element: JsonElement): String {
            return element.asJsonPrimitive.asString
        }
    }

    class SerializableI : Metadata.Serializable<Int> {
        override fun type(): Class<Int> {
            return Int::class.java
        }

        override fun decode(element: JsonElement): Int {
            return element.asJsonPrimitive.asInt
        }
    }

    class SerializableL : Metadata.Serializable<Long> {
        override fun type(): Class<Long> {
            return Long::class.java
        }

        override fun decode(element: JsonElement): Long {
            return element.asJsonPrimitive.asLong
        }

    }

    class SerializableF : Metadata.Serializable<Float> {
        override fun type(): Class<Float> {
            return Float::class.java
        }

        override fun decode(element: JsonElement): Float {
            return element.asJsonPrimitive.asFloat
        }
    }

    class SerializableD : Metadata.Serializable<Double> {
        override fun type(): Class<Double> {
            return Double::class.java
        }

        override fun decode(element: JsonElement): Double {
            return element.asJsonPrimitive.asDouble
        }
    }

    class SerializableB : Metadata.Serializable<Boolean> {
        override fun type(): Class<Boolean> {
            return Boolean::class.java
        }

        override fun decode(element: JsonElement): Boolean {
            return element.asJsonPrimitive.asBoolean
        }
    }

}
