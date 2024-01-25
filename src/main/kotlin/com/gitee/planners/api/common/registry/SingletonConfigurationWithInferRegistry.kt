package com.gitee.planners.api.common.registry

import org.bukkit.entity.Player
import taboolib.library.configuration.ConfigurationSection

abstract class SingletonConfigurationWithInferRegistry<T>(path: String)
    : SingletonConfigurationRegistry<T>(path) where T : SingletonConfigurationWithInferRegistry.Infer, T : Unique {

    private var sorted: List<T> = emptyList()


    fun getWith(player: Player): T? {
        if (this.sorted.isEmpty()) {
            return null
        }

        return this.sorted.firstOrNull { it.accept(player) } ?: getDefaultNode()
    }

    private fun getDefaultNode(): T {
        return this.getValues().firstOrNull { it.default } ?: error("No default configuration with $path")
    }

    override fun load(config: ConfigurationSection) {
        try {
            super.load(config)
            this.sorted = this.getValues().sortedBy { it.priority }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    interface Infer {

        val priority: Double

        val default: Boolean

        fun accept(player: Player): Boolean

    }


}
