package com.gitee.planners

import com.gitee.planners.api.Registries
import com.gitee.planners.api.common.util.SelectionOcclusion
import com.gitee.planners.core.attribute.AttributeProxy
import com.gitee.planners.core.attribute.source.HookAttributeSource
import com.gitee.planners.core.condition.ConditionFactory
import com.gitee.planners.core.condition.ConditionEvaluator
import com.gitee.planners.core.skill.condition.ItemCostCastCondition
import com.gitee.planners.core.config.BackpackConfig
import com.gitee.planners.core.config.SkillCategorySpec
import com.gitee.planners.core.player.magic.DefaultMagicPointProvider
import com.gitee.planners.core.skill.SkillPointsManager
import com.gitee.planners.core.skill.SkillPointsPolicy
import com.gitee.planners.core.skill.timing.SkillTimingPolicy
import com.gitee.planners.module.compat.zaphkiel.ZaphkielItemProvider
import com.gitee.planners.module.action.SelectorDebug
import com.gitee.planners.util.PhaseProfiler
import com.gitee.planners.util.configNodeToMap
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Tag
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
import java.util.logging.Level

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

    /**
     * 选区穿透白名单：这些方块不遮挡选区视线（支持方块 Tag 与 Material 名）。
     *
     * 解析结果直接写入遮挡判定，属系统级规则；未知条目按配置路径报错，不做静默忽略。
     */
    @ConfigNode("settings.selection.unimpeded-types")
    val selectionUnimpededTypes = ConfigNodeTransfer<List<String>, Set<Material>> {
        val resolved = LinkedHashSet<Material>()
        for (entry in this) {
            val text = entry.trim()
            if (text.isEmpty()) {
                continue
            }
            val keyText = if (text.startsWith("#")) text.substring(1) else text
            if (keyText.contains(':')) {
                val tag = resolveBlockTag(keyText)
                if (tag != null) {
                    resolved.addAll(tag.values)
                    continue
                }
            }
            val material = Material.matchMaterial(keyText)
            if (material == null) {
                throw IllegalArgumentException(
                    "settings.selection.unimpeded-types 无法解析 '$entry'：既不是方块 Tag 也不是方块类型。",
                )
            }
            resolved.add(material)
        }
        SelectionOcclusion.configure(resolved)
        resolved
    }

    /**
     * 选区调试总开关。
     *
     * 开启后内置选区目标器把本次判定范围绘成粒子，只发给施法玩家；属调试用途，
     * 每个选区每次执行都会发送粒子，正式使用必须关闭。
     */
    @ConfigNode("settings.debug.selector.enable")
    val selectorDebugEnabled = false

    /**
     * 选区调试使用的粒子类型。
     *
     * 按 Bukkit Particle 名称解析，未知名称在加载期直接报错，不做静默回退。
     */
    @ConfigNode("settings.debug.selector.particle")
    val selectorDebugParticle = ConfigNodeTransfer<String, Particle> {
        val normalized = trim().uppercase().replace('.', '_').replace('-', '_')
        var parsed: Particle? = null
        try {
            parsed = Particle.valueOf(normalized)
        } catch (error: IllegalArgumentException) {
            parsed = null
        }
        // 名称写错时不能悄悄换成默认粒子，否则调试结果会与配置不符。
        if (parsed == null) {
            throw IllegalArgumentException(
                "settings.debug.selector.particle 无法解析 '$this'：不是有效的 Bukkit 粒子名称。",
            )
        }
        parsed
    }

    /** 选区调试采样间距，单位格；越小越密、粒子越多。 */
    @ConfigNode("settings.debug.selector.step")
    val selectorDebugStep = 0.5

    /** 扇形调试平面的抬高量，单位格；矩形是三维框，不读取此值。 */
    @ConfigNode("settings.debug.selector.y-offset")
    val selectorDebugYOffset = 0.15

    @ConfigNode("settings.skill-points")
    val skillPointsPolicy = ConfigNodeTransfer<ConfigurationSection, SkillPointsPolicy> {
        SkillPointsPolicy.parse(this)
    }

    @ConfigNode("settings.damage-causes")
    val damageCauses = ConfigNodeTransfer<List<String>, List<String>> {
        this
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

    /**
     * 加载阶段钩子。
     *
     * 必须保留：TabooLib 的 BukkitPlugin 在静态初始化与 onLoad 两处都会注入插件实例，
     * 由本类的覆写保证顺序，删除后会触发「Plugin instance already set」而无法加载。
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
     * 解析方块 Tag。
     *
     * @param key 形如 minecraft:fences 的键。
     * @return 方块 Tag；键非法或标签不存在时返回 null。
     */
    private fun resolveBlockTag(key: String): Tag<Material>? {
        val namespaced = try {
            NamespacedKey.fromString(key)
        } catch (error: IllegalArgumentException) {
            null
        }
        if (namespaced == null) {
            return null
        }
        return Bukkit.getTag(Tag.REGISTRY_BLOCKS, namespaced, Material::class.java)
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
            com.gitee.planners.api.PlannersAPI.registerCastCondition("item_cost", ItemCostCastCondition())
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
        try {
            Registries.init()
            selectionUnimpededTypes.get()
            // 选区调试开关在业务加载完成后再同步：目标器只读取已校验的缓存值。
            applySelectorDebugConfiguration()
            reportUnresolvedActions()
            SkillPointsManager.prepare()
            DefaultMagicPointProvider.expressionUpperLimit.get()
            DefaultMagicPointProvider.expressionResume.get()
            AttributeProxy.register(HookAttributeSource())
        } catch (throwable: Throwable) {
            stopServerForConfigError(throwable)
            return
        }
        info("[Planners] 配置加载完成")
    }

    /**
     * 配置错误导致注册表只完成部分加载时立即停止服务器。
     *
     * 缺表继续运行会让等级、职业路由、键位与技能查询持续抛错，并把不完整状态写回玩家档案，
     * 因此保留原始堆栈后停服，等配置修正再启动。
     */
    private fun stopServerForConfigError(throwable: Throwable) {
        Bukkit.getLogger().log(Level.SEVERE, "[Planners] 配置加载失败，注册表处于不完整状态；为避免写坏玩家数据，服务器将立即停止。", throwable)
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getServer().shutdown()
            return
        }
        Bukkit.getScheduler().runTask(BukkitPlugin.getInstance(), Runnable {
            Bukkit.getServer().shutdown()
        })
    }

    /**
     * 把选区调试配置同步给目标器。
     *
     * 目标器绘制时只读取已经解析好的缓存值，因此启动与配置重载后各同步一次。
     * 采样间距不合法时在这里直接失败，避免带着错误配置继续运行。
     */
    fun applySelectorDebugConfiguration() {
        SelectorDebug.apply(
            selectorDebugEnabled,
            selectorDebugParticle.get(),
            selectorDebugStep,
            selectorDebugYOffset,
        )
    }

    /**
     * 插件停止时销毁运行期资源。
     */
    override fun onDisable() {
        // 先停定时器与调度器，避免技能卸载后仍有动作在跑。
        com.gitee.planners.module.action.ActionTimerService.stop()
        com.gitee.planners.module.action.AreaRegistry.clear()
        com.gitee.planners.module.action.CaptureRegistry.clear()
        com.gitee.planners.module.action.BukkitActionDispatcher.uninstall()
    }

}