package com.gitee.planners

import com.gitee.planners.api.Registries
import com.gitee.planners.api.attribute.AttributeRegistryEntry
import com.gitee.planners.core.attribute.AttributeProxy
import com.gitee.planners.core.attribute.source.HookAttributeSource
import com.gitee.planners.core.condition.ConditionConfig
import com.gitee.planners.core.condition.ConditionEvaluator
import com.gitee.planners.core.config.BackpackConfig
import com.gitee.planners.core.config.SkillCategorySpec
import com.gitee.planners.core.player.magic.DefaultMagicPointProvider
import com.gitee.planners.core.skill.SkillPointsManager
import com.gitee.planners.module.script.ScriptManager
import com.gitee.planners.module.script.PlannersCoreModule
import com.gitee.planners.module.script.YamlNovaSourceCollector
import com.gitee.planners.util.configNodeToMap
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import taboolib.common.platform.Platform
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.adaptCommandSender
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.common.platform.function.getDataFolder
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

    @Config(autoReload = false)
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

    @ConfigNode("settings.damage-causes")
    val damageCauses = ConfigNodeTransfer<List<String>, List<String>> {
        this
    }

    @ConfigNode("settings.debug.selector.sector.enable")
    val sectorSelectorDebug = false

    @ConfigNode("settings.debug.selector.sector.particle")
    val sectorSelectorDebugParticle = ConfigNodeTransfer<String, Particle> {
        try {
            Particle.valueOf(uppercase().replace(".", "_").replace("-", "_"))
        } catch (e: Exception) {
            warning("Unknown sector selector debug particle: $this")
            Particle.END_ROD
        }
    }

    @ConfigNode("settings.debug.selector.sector.step")
    val sectorSelectorDebugStep = 0.5

    @ConfigNode("settings.debug.selector.sector.y-offset")
    val sectorSelectorDebugYOffset = 0.15

    @ConfigNode("settings.skill-points.bonuses")
    val skillPointsBonuses = configNodeToMap { key, value ->
        Pair(key.toInt(), value as String)
    }

    @ConfigNode("settings.skill.categories")
    val skillCategorySpecs = configNodeToMap { key, value ->
        val cfg = value as ConfigurationSection
        val name = cfg.getString("name")
        if (name == null || name.isBlank()) {
            throw IllegalArgumentException("Skill category '$key' requires a valid name")
        }
        SkillCategorySpec(key, name)
    }

    @ConfigNode("settings.keybinding.backpack")
    val backpackConfig = ConfigNodeTransfer<ConfigurationSection, BackpackConfig> {
        BackpackConfig(this, skillCategorySpecs.get())
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

    @ConfigNode("settings.attribute.registry")
    val attributeRegistry = configNodeToMap { key, value ->
        val cfg = value as ConfigurationSection
        val name = cfg.getString("name") ?: key
        val mappingsSection = cfg.getConfigurationSection("mappings")
        val mappings: Map<String, Double>
        if (mappingsSection != null) {
            mappings = mappingsSection.getValues(false).mapValues { (it.value as Number).toDouble() }
        } else {
            mappings = emptyMap()
        }
        AttributeRegistryEntry(key, name, mappings)
    }

    /**
     *  _____  _                                  __      ____
     *  |  __ \| |                                 \ \    / /__ \
     *  | |__) | | __ _ _ __  _ __   ___ _ __ ___   \ \  / / __) |
     *  |  ___/| |/ _` | '_ \| '_ \ / _ \ '__/ __|   \ \/ / |__ <
     *  | |    | | (_| | | | | | | |  __/ |  \__ \    \  /  ___) |
     *  |_|    |_|\__,_|_| |_|_| |_|\___|_|  |___/     \/  |____/
     */
    override fun onLoad() {
        PlannersCoreModule.register()
    }

    override fun onEnable() {
        Metrics(15573, BukkitPlugin.getInstance().description.version, Platform.BUKKIT)
        LOGO.forEach(::info)
        ScriptManager.prepare()
        ConditionEvaluator.resetConfiguredProperties()
        YamlNovaSourceCollector.collect(getDataFolder().toPath())
        val configuredConditions = conditions.get()
        for (condition in configuredConditions.values) {
            condition.registerSources()
        }
        ConditionEvaluator.prepareConfiguredProperties(configuredConditions.values)
        Registries.init()
        SkillPointsManager.prepareSources()
        DefaultMagicPointProvider.expressionUpperLimit.get()
        DefaultMagicPointProvider.expressionResume.get()
        AttributeProxy.register(HookAttributeSource())

    }

    /**
     * 在全部配置 Registry 完成 ACTIVE 加载后预编译并激活 Nova Workspace。
     */
    override fun onActive() {
        // load 会解析全部物理入口与配置构造阶段登记的虚拟 SourceUnit。
        ScriptManager.load()
        info("[Planners Nova] Workspace is active")
    }

    /**
     * 插件停止时销毁 Workspace 资源树。
     */
    override fun onDisable() {
        // 业务作用域和持久作用域均由 Workspace 一次性释放。
        ScriptManager.dispose()
        PlannersCoreModule.unregister()
    }

}
