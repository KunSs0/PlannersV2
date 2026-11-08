package com.gitee.planners

import com.gitee.planners.api.Registries
import com.gitee.planners.api.attribute.AttributeRegistryEntry
import com.gitee.planners.core.attribute.AttributeProxy
import com.gitee.planners.core.attribute.source.HookAttributeSource
import com.gitee.planners.core.condition.ConditionFactory
import com.gitee.planners.core.condition.ConditionEvaluator
import com.gitee.planners.core.skill.condition.HolyEmblemCastCondition
import com.gitee.planners.core.config.BackpackConfig
import com.gitee.planners.core.config.SkillCategorySpec
import com.gitee.planners.core.player.magic.DefaultMagicPointProvider
import com.gitee.planners.core.skill.SkillPointsManager
import com.gitee.planners.core.skill.SkillPointsPolicy
import com.gitee.planners.core.skill.timing.SkillTimingPolicy
import com.gitee.planners.module.compat.zaphkiel.ZaphkielItemProvider
import com.gitee.planners.util.PhaseProfiler
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

    @ConfigNode("settings.skill-points")
    val skillPointsPolicy = ConfigNodeTransfer<ConfigurationSection, SkillPointsPolicy> {
        SkillPointsPolicy.parse(this)
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
        // 动作调度器必须在注册表加载前安装，否则 server 流程与定时器无法开工。
        com.gitee.planners.module.action.BukkitActionDispatcher.install()
    }

    /**
     * 汇报仍然无法解析的外部动作引用。
     *
     * 技能里写了某个插件提供的动作、但该插件未安装或未注册时，只记录告警，
     * 不让整个插件加载失败；真正执行到该行时才会抛错。
     */
    private fun reportUnresolvedActions() {
        val unresolved = com.gitee.planners.module.action.ActionRegistry.verifyDeferred()
        if (unresolved.isEmpty()) {
            return
        }
        warning("[Planners] 有 " + unresolved.size + " 处动作无法解析，检查对应插件是否安装：")
        for (entry in unresolved) {
            warning("  " + entry)
        }
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
            com.gitee.planners.api.PlannersAPI.registerCastCondition(HolyEmblemCastCondition.ID, HolyEmblemCastCondition())
        } catch (error: LinkageError) {
            warning("Zaphkiel 物品库 API 缺失或不匹配，zap: 物品条件将始终不满足", error)
        }
    }

    /**
     * 在全部配置 Registry 完成加载后收尾启动。
     */
    override fun onActive() {
        // 配置在下一 tick 加载：此时全部插件已完成 onEnable，跨插件动作与触发器都已注册，
        // 技能里的 fc.* / mm.* 才能解析成功。
        // 物品库适配器必须在条件解析前注册，缺失时只影响对应物品条件。
        registerItemProviders()
        Registries.init()
        reportUnresolvedActions()
        SkillPointsManager.prepare()
        DefaultMagicPointProvider.expressionUpperLimit.get()
        DefaultMagicPointProvider.expressionResume.get()
        AttributeProxy.register(HookAttributeSource())
        info("[Planners] 配置加载完成")
    }

    /**
     * 插件停止时销毁运行期资源。
     */
    override fun onDisable() {
        // 先停定时器与调度器，避免技能卸载后仍有动作在跑。
        com.gitee.planners.module.action.ActionTimerService.stop()
        com.gitee.planners.module.action.BukkitActionDispatcher.uninstall()
    }

}