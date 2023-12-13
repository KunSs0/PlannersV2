package com.gitee.planners.api.common

import taboolib.module.configuration.Configuration
import java.io.File

interface Registry<K, V> {

    fun getOrNull(id: K): V?

    fun get(id: K): V

    operator fun set(id: K, value: V)

    fun getValues(): List<V>

    fun getKeys(): Set<K>

}
