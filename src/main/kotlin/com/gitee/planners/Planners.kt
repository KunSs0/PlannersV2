package com.gitee.planners

import com.gitee.planners.api.Registries
import com.gitee.planners.core.condition.ConditionConfig
import com.gitee.planners.core.config.BackpackConfig
import com.gitee.planners.util.configNodeToMap
import org.bukkit.Bukkit
import org.bukkit.Material
import taboolib.common.platform.Platform
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.adaptCommandSender
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.ConfigNodeTransfer
import taboolib.module.configuration.Configuration
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.metrics.Metrics
import taboolib.platform.BukkitPlugin

object Planners : Plugin() {

    private const val GREEN = "\u001B[32m"
    private const val YELLOW = "\u001B[33m"
    private const val RESET = "\u001B[0m"

    val LOGO = listOf(
        "$GREEN┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$RESET",
        "$GREEN┃$YELLOW _____  _                                  __      ____$RESET",
        "$GREEN┃$YELLOW |  __ \\| |                                 \\ \\    / /__ \\$RESET",
        "$GREEN┃$YELLOW | |__) | | __ _ _ __  _ __   ___ _ __ ___   \\ \\  / / __) |$RESET",
        "$GREEN┃$YELLOW |  ___/| |/ _` | '_ \\| '_ \\ / _ \\ '__/ __|   \\ \\/ / |__ <$RESET",
        "$GREEN┃$YELLOW | |    | | (_| | | | | | | |  __/ |  \\__ \\    \\  /  ___) |$RESET",
        "$GREEN┃$YELLOW |_|    |_|\\__,_|_| |_|_| |_|\\___|_|  |___/     \\/  |____/$RESET",
        "$GREEN┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━$RESET"
    )

    @Config(autoReload = true)
    lateinit var config: Configuration
        private set

    @ConfigNode("settings.bukkit-launch.unimpeded-types")
    val unimpededTypes = ConfigNodeTransfer<List<String>, List<Material>> {
        this.mapNotNull {
            try {
                Material.valueOf(it.uppercase().replace(".", "_"))
            }catch (e: Exception) {
                warning("Unknown material type: $it")
                return@mapNotNull null
            }
        }
    }

    @ConfigNode("settings.skill-points.per-level")
    val skillPointsPerLevel = ConfigNodeTransfer<String, String> {
        this
    }

    @ConfigNode("settings.skill-points.bonuses")
    val skillPointsBonuses = configNodeToMap { key, value ->
        Pair(key.toInt(), value as String)
    }

    @ConfigNode("settings.keybinding.backpack")
    val backpackConfig = ConfigNodeTransfer<ConfigurationSection, BackpackConfig> {
        BackpackConfig(this)
    }

    @ConfigNode("settings.condition")
    val conditions = configNodeToMap { key, value ->
        val cfg = value as ConfigurationSection
        val exper = cfg.getString("exper") ?: error("Condition '$key' missing 'exper'")
        val hint = cfg.getString("hint") ?: error("Condition '$key' missing 'hint'")
        val propsSection = cfg.getConfigurationSection("props")
        val props: Map<String, Any>
        if (propsSection != null) {
            props = propsSection.getValues(false).mapValues { it.value ?: "" }
        } else {
            props = emptyMap()
        }
        val consumeStr = cfg.getString("consume")
        val consume: String?
        if (consumeStr != null && consumeStr.isNotEmpty()) {
            consume = consumeStr
        } else {
            consume = null
        }
        ConditionConfig(key, exper, props, hint, consume)
    }

    /**
     *  _____  _                                  __      ____
     *  |  __ \| |                                 \ \    / /__ \
     *  | |__) | | __ _ _ __  _ __   ___ _ __ ___   \ \  / / __) |
     *  |  ___/| |/ _` | '_ \| '_ \ / _ \ '__/ __|   \ \/ / |__ <
     *  | |    | | (_| | | | | | | |  __/ |  \__ \    \  /  ___) |
     *  |_|    |_|\__,_|_| |_|_| |_|\___|_|  |___/     \/  |____/
     */
    override fun onEnable() {
        Metrics(15573, BukkitPlugin.getInstance().description.version, Platform.BUKKIT)
        LOGO.forEach(::info)
        Registries.init()
    }

}
