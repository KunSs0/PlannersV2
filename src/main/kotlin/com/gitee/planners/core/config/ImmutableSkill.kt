package com.gitee.planners.core.config

import com.gitee.planners.Planners
import com.gitee.planners.api.common.Unique
import com.gitee.planners.api.directing.DirectingDefinition
import com.gitee.planners.api.directing.DirectingResult
import com.gitee.planners.api.directing.DirectingProviderRegistry
import com.gitee.planners.core.skill.context.SkillContext
import com.gitee.planners.core.skill.directing.DirectingConfigSection
import com.gitee.planners.api.job.Variable
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.script.NovaScriptUnit
import com.gitee.planners.module.script.ScriptManager
import com.gitee.planners.module.script.ScriptOptions
import com.gitee.planners.module.script.SingletonScript
import com.gitee.planners.util.getOption
import com.gitee.planners.util.mapValueWithId
import taboolib.library.configuration.ConfigurationSection
import taboolib.library.xseries.getItemStack
import taboolib.module.configuration.Configuration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.Locale
import java.util.regex.Pattern

class ImmutableSkill(config: Configuration) : Unique {

    /** 技能ID */
    override val id = config.file!!.nameWithoutExtension

    /** 技能名称 */
    val name: String = config.getString("__option__.name", id)!!

    private val option = config.getOption()

    /**
     * 指向性技能定义。
     *
     * 该字段在技能加载阶段按 `__option__.directing.type` 找到对应 provider 后完成解析，
     * 普通技能没有该配置时保持 null。
     */
    val directing: DirectingDefinition?
    init {
        val directingSection = option.getConfigurationSection("directing")
        if (directingSection == null) {
            directing = null
        } else {
            val type = directingSection.getString("type")
            if (type == null || type.isBlank()) {
                throw IllegalArgumentException("Skill '$id' directing configuration is missing type")
            }
            val provider = DirectingProviderRegistry.getOrNull(type)
            if (provider == null) {
                throw IllegalArgumentException("Skill '$id' directing type is not registered: $type")
            }
            val config = DirectingConfigSection(directingSection)
            val decoded = provider.decode(config)
            if (decoded.type != type) {
                throw IllegalArgumentException("Skill '$id' directing provider returned a mismatched type: ${decoded.type}")
            }
            directing = decoded
        }
    }

    /**
     * 技能图标材质的 namespaced ID。
     *
     * 该字段从配置文本直接投影，供 Zeus 等不需要 Bukkit ItemStack 的调用方使用。
     */
    val iconItemId: String? = toItemId(option.getString("display.icon.material"))

    /**
     * 技能 Bukkit 图标。
     *
     * 仅在传统 Bukkit GUI 实际需要物品对象时解码，避免配置加载和纯数据快照创建 ItemStack。
     */
    val icon
        get() = option.getConfigurationSection("display")?.getItemStack("icon")

    /** 技能图标显示名称原文，用于不创建 ItemStack 的文本渲染。 */
    val displayIconName: String? = option.getString("display.icon.name")

    /** 技能图标 Lore 原文，用于不创建 ItemStack 的文本渲染。 */
    val displayIconLore: List<String> = option.getStringList("display.icon.lore")

    /** 技能分类 */
    val categories: Set<String>
    init {
        val categoryValue = option["category"]
        categories = SkillCategories.parse(categoryValue, "技能 '$id'")
        SkillCategories.validate(categories, Planners.skillCategorySpecs.get(), "技能 '$id'")
    }

    /** 技能是否异步运行 */
    val async = option.getBoolean("async", true)

    /** 脚本源代码 */
    val action = config.getString("action", config.getString("run", ""))!!

    /**
     * 技能提供的属性。
     * key = 属性键（在 registry 中为逻辑属性，否则为物理直通）
     * value = Nova 表达式字符串或数字
     */
    val attributes: Map<String, String>
        get() {
            val section = option.getConfigurationSection("hook.attributes")
            if (section == null) {
                return emptyMap()
            }
            return section.getValues(false).mapValues { it.value.toString() }
        }

    /** 开始等级 */
    val startedLevel = option.getInt("started-level", 0)

    /** 最高等级 */
    val maxLevel = option.getInt("max-level", 10)

    val immutableVariables = option.mapValueWithId("variables") { id: String, value: Any ->
        ImmutableVariable.parse(id, value)
    }

    /**
     * 图标显示变量的单次批处理函数。
     *
     * 所有值在函数局部作用域按 YAML 声明顺序计算，避免向长期 Context 写入玩家变量。
     */
    private val displayVariablesFunction: NovaScriptUnit = createDisplayVariablesFunction()

    /** 技能 action 对应的启动期预编译 Nova 模块。 */
    private val actionModule: NovaScriptUnit?
    init {
        if (action.isBlank()) {
            actionModule = null
        } else {
            actionModule = ScriptManager.compileModule("skill:$id:action", action)
        }
    }

    /** 启动期预编译的技能属性 Nova 表达式。 */
    val attributeScripts: Map<String, SingletonScript> = attributes.mapValues { entry ->
        SingletonScript(entry.value, "skill:$id:attribute:${entry.key}")
    }

    /** 显示模板表达式到启动期预编译 Nova SourceUnit 的映射。 */
    private val templateScripts = ConcurrentHashMap<String, NovaScriptUnit>()

    init {
        registerDisplayTemplate("display.icon.name", displayIconName)
        for (index in displayIconLore.indices) {
            registerDisplayTemplate("display.icon.lore[$index]", displayIconLore[index])
        }
    }

    /** 外部插件实现的 Hook 标记接口 */
    interface Hook

    /** Hook 解码器：从配置段解析出 Hook 实例 */
    fun interface Decoder {
        fun decode(section: ConfigurationSection): Hook
    }

    /** 懒加载 hooks，首次访问时遍历 YAML __option__.hook.* 并用注册的 Decoder 解码 */
    val hooks: Map<String, Hook> by lazy {
        val section = config.getConfigurationSection("__option__.hook")
        if (section == null) {
            emptyMap<String, Hook>()
        } else {
            val result = linkedMapOf<String, Hook>()
            for (ns in section.getKeys(false)) {
                val nsSection = section.getConfigurationSection(ns)
                if (nsSection == null) {
                    continue
                }
                val decoder = decoders[ns]
                if (decoder == null) {
                    continue
                }
                result[ns] = decoder.decode(nsSection)
            }
            result
        }
    }

    companion object {
        private val decoders = ConcurrentHashMap<String, Decoder>()
        val displayTemplatePattern: Pattern = Pattern.compile("\\{\\{(.+?)}}")

        fun registerHook(namespace: String, decoder: Decoder) {
            decoders[namespace] = decoder
        }
    }

    /**
     * 执行技能脚本
     */
    fun execute(
        sender: ProxyTarget<*>,
        level: Int = 0,
        variables: Map<String, Any?> = emptyMap()
    ): CompletableFuture<Any?> {
        if (action.isEmpty()) {
            return CompletableFuture.completedFuture(null)
        }

        val options = ScriptOptions.forSkill(sender, level, this@ImmutableSkill)
        options.set("origin", (sender as? ProxyTarget.Location<*>)?.getBukkitLocation())
        val directing = variables["directing"]
        if (directing is DirectingResult) {
            val context = options.variables["ctx"]
            if (context is SkillContext) {
                context.directing = directing
            }
        }
        for ((k, v) in variables) {
            options.set(k, v)
        }

        val task = {
            // 全部 YAML variable 表达式先由同一 Workspace 的预编译 Nova 批处理入口计算。
            val resolvedVariables = evaluateDisplayVariables(options)
            for ((variableId, variableValue) in resolvedVariables) {
                options.set(variableId, variableValue)
            }
            val session = ScriptManager.openSession(options)
            try {
                val module = actionModule
                if (module == null) {
                    null
                } else {
                    session.invoke(module, "execute")
                }
            } finally {
                session.close()
            }
        }

        return if (async) {
            CompletableFuture.supplyAsync(task)
        } else {
            CompletableFuture.completedFuture(task())
        }
    }

    /**
     * 调用当前技能 action 模块中的业务回调。
     *
     * @param functionName action 模块导出的函数名。
     * @param options 本次技能调用绑定。
     * @param payload 回调负载。
     * @return Nova 回调返回值；技能未声明 action 时返回 null。
     */
    fun invokeActionFunction(functionName: String, options: ScriptOptions, payload: Map<String, Any>): Any? {
        val module = actionModule
        if (module == null) {
            return null
        }
        val session = ScriptManager.openSession(options)
        try {
            return session.invoke(module, functionName, payload)
        } finally {
            session.close()
        }
    }

    fun getVariableOrNull(id: String): Variable? {
        return immutableVariables[id]
    }

    fun getVariables(): Map<String, Variable> {
        return immutableVariables
    }

    fun evaluateDisplayVariables(options: ScriptOptions): Map<String, Any?> {
        val values = ScriptManager.invokePersistent(
            displayVariablesFunction,
            options,
            options.variables["sender"],
            options.variables["level"],
            options.variables["ctx"],
            options.variables["skill"],
            options.variables["profile"]
        )
        if (values !is List<*>) {
            throw IllegalStateException("Skill '$id' display variable batch returned an invalid type")
        }
        if (values.size != immutableVariables.size) {
            throw IllegalStateException("Skill '$id' display variable batch returned an invalid value count")
        }
        val result = LinkedHashMap<String, Any?>()
        var index = 0
        for ((variableId, _) in immutableVariables) {
            result[variableId] = values[index]
            index += 1
        }
        return result
    }

    /**
     * 使用 RuntimeWorkspace 预编译入口计算一个 `{{expression}}`。
     *
     * @param expression 模板中的原始 Nova 表达式。
     * @param options 已包含技能变量与调用上下文的绑定。
     * @return Nova 表达式结果。
     */
    fun evaluateDisplayTemplate(expression: String, options: ScriptOptions): Any? {
        val normalized = expression.trim()
        val script = templateScripts[normalized]
        if (script == null) {
            throw IllegalStateException("Skill '$id' display template was not precompiled: {{$normalized}}")
        }
        return ScriptManager.invokePersistent(script, options)
    }

    /** 扫描一项显示文本，并在 Workspace load 前登记其中全部 Nova 模板表达式。 */
    private fun registerDisplayTemplate(path: String, text: String?) {
        if (text == null) {
            return
        }
        val matcher = displayTemplatePattern.matcher(text)
        var expressionIndex = 0
        while (matcher.find()) {
            val expression = matcher.group(1).trim()
            if (expression.isEmpty()) {
                throw IllegalArgumentException("Skill '$id' has a blank display template at $path")
            }
            val sourceId = "skill:$id:template:$path:$expressionIndex"
            val script = ScriptManager.compileExpression(sourceId, expression)
            templateScripts[expression] = script
            expressionIndex += 1
        }
    }

    private fun createDisplayVariablesFunction(): NovaScriptUnit {
        val body = StringBuilder()
        for ((_, variable) in immutableVariables) {
            variable.appendDisplayEvaluation(body)
        }
        body.append("return [")
        var first = true
        for ((variableId, _) in immutableVariables) {
            if (!first) {
                body.append(',')
            }
            body.append(variableId)
            first = false
        }
        body.append("]\n")
        return ScriptManager.compileAction(
            "skill:$id:display-variables",
            listOf("sender", "level", "ctx", "skill", "profile"),
            body.toString()
        )
    }

    /**
     * 将技能图标材质文本转换为 Minecraft namespaced ID。
     *
     * @param material 配置中的 Bukkit Material 名称。
     * @return 小写 namespaced ID；未配置时返回 null。
     */
    private fun toItemId(material: String?): String? {
        if (material == null) {
            return null
        }
        val normalized = material.trim()
        if (normalized.isEmpty()) {
            return null
        }
        if (normalized.contains(':')) {
            return normalized.lowercase(Locale.ROOT)
        }
        return "minecraft:" + normalized.lowercase(Locale.ROOT)
    }

    override fun toString(): String = "ImmutableSkill(id='$id', action='$action', startedLevel=$startedLevel, immutableVariables=$immutableVariables)"

}
