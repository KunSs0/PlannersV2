package com.gitee.planners.api.common.registry

import taboolib.library.configuration.ConfigurationSection

abstract class AbstractConfigurationRegistry<T : Unique,C: ConfigurationSection> : AbstractRegistry<String, T>() {

    open fun load(config: C) {
        try {
            val instance = invokeInstance(config)
            this[instance.id] = instance
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    abstract fun invokeInstance(config: C): T

}
