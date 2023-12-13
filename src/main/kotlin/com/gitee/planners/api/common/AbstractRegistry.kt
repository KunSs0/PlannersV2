package com.gitee.planners.api.common

abstract class AbstractRegistry<K, V> : Registry<K, V> {

    val table = mutableMapOf<K, V>()

    override fun getOrNull(id: K): V? {
        return table[id]
    }

    override fun get(id: K): V {
        return getOrNull(id) ?: error("Find not found for id $id")
    }

    override fun set(id: K, value: V) {
        this.table[id] = value
    }

    override fun getValues(): List<V> {
        return table.values.toList()
    }

    override fun getKeys(): Set<K> {
        return table.keys
    }
}
