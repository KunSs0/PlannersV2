package com.gitee.planners.api.common.registry

abstract class AbstractRegistry<K, V> : Registry<K, V> {

    protected val table = LinkedHashMap<K, V>()

    override fun getOrNull(id: K): V? {
        return table[id]
    }

    override fun getSize(): Int {
        return table.size
    }

    override operator fun get(id: K): V {
        return getOrNull(id) ?: error("Find not found for id $id")
    }

    override operator fun set(id: K, value: V) {
        this.table[id] = value
    }

    override fun getValues(): List<V> {
        return table.values.toList()
    }

    fun computeIfAbsent(key: K, func: () -> V): V {
        return table.computeIfAbsent(key) { func() }
    }

    override fun getKeys(): Set<K> {
        return table.keys
    }

    override fun containsKey(key: K): Boolean {
        return table.containsKey(key)
    }

    override fun removeAll() {
        this.table.clear()
    }

    override fun remove(key: K) {
        this.table.remove(key)
    }


    override fun removeIf(func: (key: K, value: V) -> Boolean) {
        this.table.filter { func(it.key, it.value) }.forEach {
            this.table.remove(it.key)
        }
    }

    override fun toString(): String {
        return "AbstractRegistry(table=$table)"
    }

    override fun toMap(): Map<K, V> {
        return table.toMap()
    }

}
