package com.gitee.planners.api.common

import taboolib.module.configuration.Configuration

abstract class AbstractRegistryBuiltin<T : Unique> : Registry<T> {

    protected val table = mutableMapOf<String, T>()

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
