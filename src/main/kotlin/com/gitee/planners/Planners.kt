package com.gitee.planners

import org.bukkit.Material
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.ConfigNodeTransfer
import taboolib.module.configuration.Configuration

object Planners : Plugin() {

    @Config(autoReload = true)
    lateinit var config: Configuration
        private set

    @ConfigNode("settings.bukkit-launch.unimpeded-types")
    val unimpededTypes = ConfigNodeTransfer<List<String>,List<Material>> {
        this.map { Material.valueOf(it.uppercase().replace(".","_")) }
    }

    override fun onEnable() {
        info("Hello TabooLib")
    }

}
