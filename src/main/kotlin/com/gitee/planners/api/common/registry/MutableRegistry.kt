package com.gitee.planners.api.common.registry

open class MutableRegistry<K, V>(prev: Map<K, V> = emptyMap()) : AbstractRegistry<K, V>() {

    init {
        // 合并
        prev.forEach { this.set(it.key, it.value) }
    }


}
