package com.gitee.planners.core.database

import taboolib.library.configuration.ConfigurationSection

class DatabaseOption(val config: ConfigurationSection) {

    val use = config.getString("use", "LOCAL")!!

    val sql = config.getConfigurationSection("sql")

}
