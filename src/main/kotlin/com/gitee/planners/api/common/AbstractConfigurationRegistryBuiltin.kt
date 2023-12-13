package com.gitee.planners.api.common

import taboolib.module.configuration.Configuration
import java.io.File

abstract class AbstractConfigurationRegistryBuiltin<T : Unique> : AbstractRegistry<String, T>() {

    fun loadFromFile(file: File) {
        this.load(Configuration.loadFromFile(file))
    }

    fun load(config: Configuration) {
        try {
            val instance = invokeInstance(config)
            this[instance.id] = instance
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    abstract fun invokeInstance(config: Configuration): T

}
