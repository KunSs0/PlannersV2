package com.gitee.planners.api.common.metadata

import com.google.gson.JsonElement
import org.bukkit.Location
import taboolib.common5.*
import java.nio.charset.StandardCharsets

class MetadataTypeToken {

    class Void : TypeToken(String::class.java, Any(), -1) {

        override fun isTimeout(): Boolean {
            return true
        }

    }

    open class TypeToken(override val clazz: Class<*>, val any: Any, override val stopTime: Long) : Metadata {

        open override fun isTimeout(): Boolean {
            return System.currentTimeMillis() > stopTime
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

    class SerializableB : Metadata.Serializable<Boolean> {
        override fun type(): Class<Boolean> {
            return Boolean::class.java
        }

        override fun decode(element: JsonElement): Boolean {
            return element.asJsonPrimitive.asBoolean
        }
    }

}
