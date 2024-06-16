package com.gitee.planners

import com.gitee.planners.api.Registries
import org.bukkit.Bukkit
import org.bukkit.Material
import taboolib.common.platform.Platform
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.adaptCommandSender
import taboolib.common.platform.function.info
import taboolib.module.chat.colored
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.ConfigNodeTransfer
import taboolib.module.configuration.Configuration
import taboolib.module.metrics.Metrics
import taboolib.platform.BukkitPlugin

object Planners : Plugin() {

    val LOGO = listOf(
        "&a┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        "&a┃&e_____  _                                  __      _____",
        "&a┃&e|  __ \\| |                                 \\ \\    / /__ \\",
        "&a┃&e| |__) | | __ _ _ __  _ __   ___ _ __ ___   \\ \\  / /   ) |",
        "&a┃&e|  ___/| |/ _` | '_ \\| '_ \\ / _ \\ '__/ __|   \\ \\/ /   / /",
        "&a┃&e| |    | | (_| | | | | | | |  __/ |  \\__ \\    \\  /   / /_",
        "&a┃&e|_|    |_|\\__,_|_| |_|_| |_|\\___|_|  |___/     \\/   |____|",
        "&a┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    )

    @Config(autoReload = true)
    lateinit var config: Configuration
        private set

    @ConfigNode("settings.bukkit-launch.unimpeded-types")
    val unimpededTypes = ConfigNodeTransfer<List<String>, List<Material>> {
        this.map { Material.valueOf(it.uppercase().replace(".", "_")) }
    }

    /**
     *  _____  _                                  __      _____
     *  |  __ \| |                                 \ \    / /__ \
     *  | |__) | | __ _ _ __  _ __   ___ _ __ ___   \ \  / /   ) |
     *  |  ___/| |/ _` | '_ \| '_ \ / _ \ '__/ __|   \ \/ /   / /
     *  | |    | | (_| | | | | | | |  __/ |  \__ \    \  /   / /_
     *  |_|    |_|\__,_|_| |_|_| |_|\___|_|  |___/     \/   |____|
     */
    override fun onEnable() {
        Metrics(15573, BukkitPlugin.getInstance().description.version, Platform.BUKKIT)
        LOGO.colored().forEach(::info)
        Registries.init()
    }

}
