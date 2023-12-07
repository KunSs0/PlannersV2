package com.gitee.planners.util

import com.gitee.planners.Planners
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.util.mapSection


val config: Configuration
    get() = Planners.config

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
