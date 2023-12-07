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

}
