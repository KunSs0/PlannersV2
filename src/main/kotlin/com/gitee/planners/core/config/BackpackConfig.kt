package com.gitee.planners.core.config

import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.util.mapSection

class BackpackConfig(config: ConfigurationSection) {

    val defaultPage: String = config.getString("default-page", "0")!!

    val pages: Map<String, BackpackPage> =
        config.getConfigurationSection("pages")?.mapSection { BackpackPage(it) } ?: emptyMap()

    fun getPage(id: String): BackpackPage? = pages[id]

    fun getFirstPageId(): String? = pages.keys.firstOrNull()
}

class BackpackPage(config: ConfigurationSection) {

    val id: String = config.name

    val name: String = config.getString("name", id)!!

    val slots: Map<String, BackpackSlot> =
        config.getConfigurationSection("slots")?.mapSection { BackpackSlot(it) } ?: emptyMap()

    fun getSlotForKey(keyId: String): BackpackSlot? =
        slots.values.firstOrNull { it.key == keyId }
}

class BackpackSlot(config: ConfigurationSection) {

    val id: String = config.name

    val key: String = config.getString("key")!!
}
