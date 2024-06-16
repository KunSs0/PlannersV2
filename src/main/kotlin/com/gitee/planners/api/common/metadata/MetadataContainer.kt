package com.gitee.planners.api.common.metadata


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

    fun getImmutableRegistry(): Map<String, Metadata> {
        return mapOf(*table.map { it.key to it.value }.toTypedArray())
    }

    fun copyMetaDataTo(container: MetadataContainer) { // Quick clone
        if (this::class != container::class) {
            throw IllegalArgumentException("MetadataContainer type mismatch")
        }
        container.table.putAll(this.table)
    }

}
