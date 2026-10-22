package com.gitee.planners

import com.gitee.planners.api.Registries
import com.gitee.planners.api.attribute.AttributeRegistryEntry
import com.gitee.planners.core.attribute.AttributeProxy
import com.gitee.planners.core.attribute.source.HookAttributeSource
import com.gitee.planners.core.condition.ConditionFactory
import com.gitee.planners.core.condition.ConditionEvaluator
import com.gitee.planners.core.config.BackpackConfig
import com.gitee.planners.core.config.SkillCategorySpec
import com.gitee.planners.core.player.magic.DefaultMagicPointProvider
import com.gitee.planners.core.skill.SkillPointsManager
import com.gitee.planners.core.skill.timing.SkillTimingPolicy
import com.gitee.planners.module.script.ScriptManager
import com.gitee.planners.module.compat.zaphkiel.ZaphkielItemProvider
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

    /**
     * 技能时间策略，在配置加载和重载时解析，技能释放只读取已校验缓存。
     */
    @ConfigNode("settings.skill.timing")
    val skillTimingPolicy = ConfigNodeTransfer<ConfigurationSection, SkillTimingPolicy> {
        SkillTimingPolicy.parse(this)
    }

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
        ConditionFactory.create(key, value as ConfigurationSection)
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
    }

    override fun onEnable() {
        // 时间策略必须在技能可被释放前明确配置，不能在缺项时沿用原时间。
        skillTimingPolicy.get()
        Metrics(15573, BukkitPlugin.getInstance().description.version, Platform.BUKKIT)
        LOGO.forEach(::info)
        ScriptManager.prepare()
        YamlNovaSourceCollector.collect(getDataFolder().toPath())
        // 物品库适配器必须在条件解析前注册，缺失时只影响对应物品条件。
        registerItemProviders()
        Registries.init()
        SkillPointsManager.prepareSources()
        DefaultMagicPointProvider.expressionUpperLimit.get()
        DefaultMagicPointProvider.expressionResume.get()
        AttributeProxy.register(HookAttributeSource())

    }

    /**
     * 注册可选物品库适配器。
     *
     * 适配器只在对应物品库插件已启用时注册，避免在未安装时触发类链接错误；
     * 未注册的物品库引用由条件求值阶段按不满足处理。
     */
    private fun registerItemProviders() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Zaphkiel")) {
            return
        }
        try {
            ZaphkielItemProvider.register()
        } catch (error: LinkageError) {
            warning("Zaphkiel 物品库 API 缺失或不匹配，zap: 物品条件将始终不满足", error)
        }
    }

    /**
     * 在全部配置 Registry 完成 ACTIVE 加载后预编译并激活 Nova 编译程序。
     */
    override fun onActive() {
        // load 会解析全部物理入口与配置构造阶段登记的虚拟 SourceUnit。
        ScriptManager.load()
        info("[Planners Nova] Program is active")
    }

    /**
     * 插件停止时销毁编译程序资源。
     */
    override fun onDisable() {
        // 全部回调与任务由编译程序一次性释放。
        ScriptManager.close()
    }

}
