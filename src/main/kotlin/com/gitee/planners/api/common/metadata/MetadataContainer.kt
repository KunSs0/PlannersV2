package com.gitee.planners.api.common.metadata

import com.gitee.planners.api.common.registry.MutableRegistry
import com.gitee.planners.api.common.registry.mutableRegistry

abstract class MetadataContainer(map: Map<String, Metadata> = emptyMap()) {

    private val table = mutableMapOf(*map.map { it.key to it.value }.toTypedArray())

    private val changed = mutableSetOf<String>()

    operator fun get(key: String): Metadata? {
        val metadata = table[key]
        if (metadata != null && !metadata.isTimeout()) {
            return metadata
        }

        return null
    }

    operator fun set(key: String, value: Metadata) {
        this.table[key] = value
        changed += key
    }

    fun release(): Map<String, Metadata> {
        val metadataMap = changed.associateWith { get(it) ?: MetadataTypeToken.Void() }
        // 清空缓冲区
        this.changed.clear()
        return metadataMap
    }

    fun removeAll() {
        // 设置为虚空节点
        this.table.keys.forEach {
            this[it] = MetadataTypeToken.Void()
        }
    }

    fun getImmutableRegistry(): MutableRegistry<String, Metadata> {
        return mutableRegistry(table.filter { !it.value.isTimeout() })
    }

}
