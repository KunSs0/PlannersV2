package com.gitee.planners.util

import com.gitee.planners.Planners
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.ConfigNodeTransfer
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.util.mapSection


val config: Configuration
    get() = Planners.config

fun <T> configNodeToMap(transfer: (id: String, value: Any) -> T): ConfigNodeTransfer<ConfigurationSection, Map<String, T>> {
    return ConfigNodeTransfer {
        this.getKeys(false).associateWith { transfer(it, this.get(it)!!) }
    }
}

fun <T> configNodeTo(transfer: ConfigurationSection.() -> T): ConfigNodeTransfer<ConfigurationSection, T> {
    return ConfigNodeTransfer { transfer(this) }
}

fun <T> configNodeToList(transfer: Any.() -> T): ConfigNodeTransfer<List<Any>, List<T>> {
    return ConfigNodeTransfer {
        this.map(transfer)
    }
}

@Suppress("UNCHECKED_CAST")
fun <V> ConfigurationSection.mapSectionNotNull(transform: (ConfigurationSection) -> V?): Map<String, V> {

    return getKeys(false)
        .map { it to transform(getConfigurationSection(it)!!) }
        .filter { it.second != null }
        .toMap() as Map<String, V>
}

fun <V> ConfigurationSection.mapValueWithId(node: String, block: (id: String, value: Any) -> V): Map<String, V> {
    mapSection { }
    return getConfigurationSection(node)?.mapValueWithId(block) ?: emptyMap()
}

fun <V> ConfigurationSection.mapValueWithId(block: (id: String, value: Any) -> V): Map<String, V> {
    return getKeys(false).associateWith { block(it, this[it]!!) }
}

fun ConfigurationSection.getOption(): ConfigurationSection {
    return this.getConfigurationSection("__option__") ?: Configuration.empty()
}
