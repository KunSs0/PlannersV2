package com.gitee.planners.api.common

import taboolib.module.configuration.Configuration
import java.io.File

interface Registry<T> {

    fun getOrNull(id: String): T?

    fun get(id: String): T

    fun loadFromFile(file: File) {
        this.load(Configuration.loadFromFile(file))
    }

    fun load(config: Configuration)

    fun getValues(): List<T>

    fun getKeys(): Set<String>

    abstract class AbstractBuiltin<T : Unique> : Registry<T> {

        private val table = mutableMapOf<String, T>()

        override fun getOrNull(id: String): T? {
            return table[id]
        }

        override fun get(id: String): T {
            return getOrNull(id) ?: error("Find not found for id $id")
        }

        override fun load(config: Configuration) {
            try {
                val instance = invokeInstance(config)
                this.table[instance.id] = instance
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun getKeys(): Set<String> {
            return table.keys
        }

        override fun getValues(): List<T> {
            return table.values.toList()
        }

        abstract fun invokeInstance(config: Configuration): T

    }

}
