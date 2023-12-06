package com.gitee.planners.util

import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration


fun <T> ConfigurationSection.mapValueWithId(node: String, block: (id: String, value: Any) -> T): Map<String, T> {
    return getConfigurationSection(node)?.mapValueWithId(block) ?: emptyMap()
}

fun <T> ConfigurationSection.mapValueWithId(block: (id: String, value: Any) -> T): Map<String, T> {
    return getKeys(false).associateWith { block(it, this[it]!!) }
}

fun ConfigurationSection.getOption(): ConfigurationSection {
    return this.getConfigurationSection("__option__") ?: Configuration.empty()
}
