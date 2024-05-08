package com.gitee.planners.api.common.metadata

import com.gitee.planners.util.unboxJavaToKotlin
import com.google.gson.*
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.common.util.Vector
import taboolib.common.util.unsafeLazy
import java.lang.reflect.Type
import java.util.function.Supplier

interface Metadata {

    val clazz: Class<*>

    val timeoutTick: Long

    fun isTimeout(): Boolean

    fun asString(): String

    fun asInt(): Int

    fun asLong(): Long

    fun asFloat(): Float

    fun asDouble(): Double

    fun asBoolean(): Boolean

    fun asVector(): Vector

    fun any(): Any

    fun increase(value: Any)

    interface Serializable<T> : JsonDeserializer<T>, JsonSerializer<T> {

        fun type(): Class<T>

        fun decode(element: JsonElement): T

        fun encode(src: T): JsonElement {
            return Loader.gson.toJsonTree(src)
        }

        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): T {
            return decode(json!!)
        }

        override fun serialize(src: T, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return encode(src)
        }

    }

    @Awake
    object Loader : ClassVisitor(0) {

        private val table = mutableMapOf<Class<*>, Serializable<*>>()

        val gson by unsafeLazy { createGson().create() }

        fun isSupported(clazz: Class<*>): Boolean {
            return this.table.containsKey(clazz)
        }

        fun toJson(metadata: Metadata): String {
            return gson.toJson(metadata.any())
        }

        fun parseTypeToken(type: Class<*>, content: String, stopTime: Long): MetadataTypeToken.TypeToken {
            val data = parseJson<Any>(type, content)
            return MetadataTypeToken.TypeToken(type, data, stopTime)
        }

        fun <T> parseJson(clazz: Class<*>, content: String): Any {
            val serializable = table[unboxJavaToKotlin(clazz)]
                    ?: throw IllegalStateException("No serializable class found for class $clazz")
            return gson.fromJson(content, serializable.type())!!
        }

        override fun getLifeCycle(): LifeCycle {
            return LifeCycle.ENABLE
        }

        fun createGson(): GsonBuilder {
            val builder = GsonBuilder()
            this.table.forEach { clazz, serializable ->
                builder.registerTypeHierarchyAdapter(clazz, serializable)
            }
            return builder
        }

        override fun visitEnd(clazz: Class<*>, instance: Supplier<*>?) {
            if (Serializable::class.java.isAssignableFrom(clazz) && !clazz.isInterface) {
                val serializable = (instance?.get() ?: clazz.newInstance()) as Serializable<*>
                table[unboxJavaToKotlin(serializable.type())] = serializable
            }
        }

    }

}
