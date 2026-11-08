package com.gitee.planners.core.config

import com.gitee.planners.Planners
import com.gitee.planners.api.common.Unique
import com.gitee.planners.api.directing.DirectingDefinition
import com.gitee.planners.api.directing.DirectingResult
import com.gitee.planners.api.directing.DirectingProviderRegistry
import com.gitee.planners.core.skill.context.SkillExecutionContext
import com.gitee.planners.core.skill.condition.CastCostTiming
import com.gitee.planners.core.skill.condition.SkillCastConditionConfig
import com.gitee.planners.core.skill.directing.DirectingConfigSection
import com.gitee.planners.api.job.Variable
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.action.ActionParser
import com.gitee.planners.module.action.ActionRunner
import com.gitee.planners.module.action.ActionScopeFactory
import com.gitee.planners.module.action.query.QueryRef
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.Player
import com.gitee.planners.module.action.ActionSpec
import com.gitee.planners.util.getOption
import com.gitee.planners.util.mapValueWithId
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

    val castConditions: List<SkillCastConditionConfig> = parseCastConditions()

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

    /**
     * 结构化动作配置节点。
     *
     * 只接受映射写法（流程名到动作列表）；写成字符串属于已移除的脚本写法，
     * 加载期直接报错，不再静默忽略。
     */
    private val actionSection = config.getConfigurationSection("action")

    /**
     * 结构化的 Action 定义。
     */
    val actionSpec: ActionSpec? = if (actionSection == null) {
        val legacy = config.getString("action", config.getString("run", ""))
        if (legacy != null && legacy.isNotBlank()) {
            throw IllegalArgumentException(
                "skill:$id.action 仍是脚本字符串写法；请改成按流程名组织的结构化动作列表。",
            )
        }
        null
    } else {
        ActionParser.parse(id, actionSection, "skill:$id.action")
    }

    /**
     * 技能提供的属性。
     * key = 属性键（在 registry 中为逻辑属性，否则为物理直通）
     * value = 查询表达式，例如 `10 + (skill.level - 1) * 1`
     */
    val attributeExpressions: Map<String, com.gitee.planners.module.action.query.QueryRef> =
        com.gitee.planners.module.action.AttributeExpressions.compileSection(option, "skill:$id")

    /** 开始等级 */
    val startedLevel = option.getInt("started-level", 0)

    /** 最高等级 */
    val maxLevel = option.getInt("max-level", 10)

    private fun parseCastConditions(): List<SkillCastConditionConfig> {
        val section = option.getConfigurationSection("condition.consume.cast")
        if (section == null) {
            return emptyList()
        }
        val result = ArrayList<SkillCastConditionConfig>()
        for (id in section.getKeys(false)) {
            if (id.contains('.')) {
                throw IllegalArgumentException("Skill '${this@ImmutableSkill.id}' cast condition id must not contain '.'")
            }
            val conditionSection = section.getConfigurationSection(id)
            if (conditionSection == null) {
                throw IllegalArgumentException("Skill '$id' cast condition must be a configuration section")
            }
            val propsSection = conditionSection.getConfigurationSection("props")
            val props = LinkedHashMap<String, Any>()
            if (propsSection != null) {
                for ((propId, propValue) in propsSection.getValues(false)) {
                    if (propValue != null) {
                        props[propId] = propValue
                    }
                }
            }
            val costSection = conditionSection.getConfigurationSection("cost")
            val timing = if (costSection == null) {
                null
            } else {
                val rawTiming = costSection.getString("timing")
                if (rawTiming == null) {
                    throw IllegalArgumentException("Skill '${this@ImmutableSkill.id}' cast condition '$id' cost is missing timing")
                }
                try {
                    CastCostTiming.valueOf(rawTiming.uppercase())
                } catch (exception: IllegalArgumentException) {
                    throw IllegalArgumentException("Skill '${this@ImmutableSkill.id}' cast condition '$id' cost timing is invalid: $rawTiming")
                }
            }
            result.add(SkillCastConditionConfig(id, props, timing))
        }
        return result
    }

    val immutableVariables = option.mapValueWithId("variables") { id: String, value: Any ->
        ImmutableVariable.parse(id, value)
    }

    init {
        // 所有技能必须明确声明施法时间；旧配置缺项不能静默当作瞬发。
        require(immutableVariables.containsKey("castTime")) { "技能${id}缺少__option__.variables.castTime；瞬发技能须显式配置0。" }
    }

    /**
     * 图标显示变量的单次批处理函数。
     *
     * 所有值在函数局部作用域按 YAML 声明顺序计算，避免向长期 Context 写入玩家变量。
     */
    

    /** 显示模板表达式到启动期预编译条件引用的映射。 */
    private val templateRefs = ConcurrentHashMap<String, QueryRef>()

    init {
        registerDisplayTemplate("display.icon.name", displayIconName)
        for (index in displayIconLore.indices) {
            registerDisplayTemplate("display.icon.lore[$index]", displayIconLore[index])
        }
    }

    /** 外部插件实现的 Hook 标记接口 */
    interface Hook

    /** Hook 解码器：从原始配置映射解析出 Hook 实例 */
    fun interface Decoder {
        fun decode(config: Map<String, Any?>): Hook
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
                result[ns] = decoder.decode(nsSection.toMap())
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
     * 执行技能的结构化动作流程。
     *
     * 只支持 `action` 结构化写法；历史脚本写法已随脚本运行时移除。
     */
    fun execute(
        sender: ProxyTarget<*>,
        level: Int = 0,
        variables: Map<String, Any?> = emptyMap()
    ): CompletableFuture<Any?> {
        val structured = actionSpec
        if (structured == null) {
            return CompletableFuture.completedFuture(null)
        }

        val execution = SkillExecutionContext.create(sender, level, this@ImmutableSkill, variables)
        execution.origin = (sender as? ProxyTarget.Location<*>)?.getBukkitLocation()
        val directing = variables["directing"]
        if (directing is DirectingResult) {
            execution.context.directing = directing
        }

        val task = {
            runStructuredFlow(structured, ActionSpec.FIRE, execution)
            null
        }

        return if (async) {
            CompletableFuture.supplyAsync(task)
        } else {
            CompletableFuture.completedFuture(task())
        }
    }

    /**
     * 执行技能里的一个命名流程。
     *
     * @param functionName 流程名。
     * @param execution 本次技能执行上下文。
     * @param payload 回调负载。
     * @return 已执行时返回 true；未声明该流程时返回 null。
     */
    fun invokeActionFunction(functionName: String, execution: SkillExecutionContext, payload: Map<String, Any>): Any? {
        val structured = actionSpec
        if (structured == null) {
            return null
        }
        // 结构化配置里回调名就是流程名，未声明该流程即视为不处理。
        if (!structured.hasFlow(functionName)) {
            return null
        }
        runStructuredFlow(structured, functionName, execution, payload)
        return true
    }

    fun getVariableOrNull(id: String): Variable? {
        return immutableVariables[id]
    }

    fun getVariables(): Map<String, Variable> {
        return immutableVariables
    }

    fun evaluateDisplayVariables(execution: SkillExecutionContext): Map<String, Any?> {
        // 变量统一由查询引擎求值，不再为每个技能编译一份 Nova 批量函数。
        return evaluateStructuredVariables(execution)
    }

    /**
     * 使用编译程序入口计算一个 `{{expression}}`。
     *
     * @param expression 模板中的原始表达式。
     * @param execution 已完成技能变量计算的执行上下文。
     * @return 表达式结果。
     */
    fun evaluateDisplayTemplate(expression: String, execution: SkillExecutionContext): Any? {
        val normalized = expression.trim()
        val ref = templateRefs[normalized]
        if (ref == null) {
            throw IllegalStateException("Skill '$id' display template was not precompiled: {{$normalized}}")
        }
        return ref.resolve(displayTemplateScope(execution))
    }

    /**
     * 构造显示模板的查询上下文。
     *
     * 模板只能读到本次执行已算出的技能变量与基础字段。
     *
     * @param execution 已完成技能变量计算的执行上下文。
     * @return 查询上下文。
     */
    private fun displayTemplateScope(execution: SkillExecutionContext): QueryScope {
        val sender = execution.sender
        val player = sender as? Player
        if (player != null) {
            return ActionScopeFactory.forPlayer(
                player,
                this,
                execution.level,
                execution.variableSnapshot(),
                null,
                "skill:$id:template",
            )
        }
        return QueryScope(
            sender = sender,
            skill = this,
            profile = execution.profile,
            variables = execution.variableSnapshot(),
            skillFieldReader = { value, field -> readDisplayField(value, field, execution.level) },
            label = "skill:$id:template",
        )
    }

    /**
     * 非玩家施法者下读取技能基础字段。
     *
     * 技能变量已放进查询上下文的 `variables`，这里只补基础字段。
     *
     * @param value 技能对象。
     * @param field 字段名。
     * @param level 技能等级。
     * @return 字段值；未命中时返回 null。
     */
    private fun readDisplayField(value: Any, field: String, level: Int): Any? {
        val skill = value as? ImmutableSkill
        if (skill == null) {
            return null
        }
        if (field == "id") {
            return skill.id
        }
        if (field == "name") {
            return skill.id
        }
        if (field == "level") {
            return level
        }
        return null
    }

    /** 扫描一项显示文本，并预编译其中全部模板表达式。 */
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
            templateRefs[expression] = QueryRef.parseExpression(expression, sourceId)
            expressionIndex += 1
        }
    }

    /**
     * 执行结构化流程。
     *
     * 变量按声明顺序求值，后声明的变量可以引用先声明的结果；求值完成后同步写入
     * 执行上下文，供技能结束或后续回调读取。
     *
     * @param spec 当前技能的结构化定义。
     * @param flowId 要执行的流程名。
     * @param execution 本次技能执行上下文。
     * @param payload 回调载荷，供流程读取播放句柄等外部数据。
     */
    private fun runStructuredFlow(
        spec: ActionSpec,
        flowId: String,
        execution: SkillExecutionContext,
        payload: Map<String, Any?> = emptyMap(),
    ) {
        val accumulated = LinkedHashMap<String, Any?>()
        val scope = ActionScopeFactory.forExecution(execution, accumulated, payload)
        for (entry in immutableVariables) {
            accumulated[entry.key] = entry.value.evaluate(scope)
        }
        // 指向结果由运行时注入，覆盖同名变量，供 `<skill.directing>` 读取。
        val directing = execution.context.directing
        if (directing != null) {
            accumulated["directing"] = directing
        }
        for (entry in accumulated) {
            execution.setVariable(entry.key, entry.value)
        }
        if (!spec.hasFlow(flowId)) {
            return
        }
        ActionRunner.run(spec, flowId, scope)
    }

    /**
     * 用查询引擎按声明顺序求值全部技能变量。
     *
     * @param execution 本次技能执行上下文。
     * @return 变量名到值的映射。
     */
    private fun evaluateStructuredVariables(execution: SkillExecutionContext): Map<String, Any?> {
        val accumulated = LinkedHashMap<String, Any?>()
        val scope = ActionScopeFactory.forExecution(execution, accumulated)
        for (entry in immutableVariables) {
            accumulated[entry.key] = entry.value.evaluate(scope)
        }
        return accumulated
    }


    /** 在已完成变量准备的上下文里执行技能主流程。 */
    fun execute(execution: SkillExecutionContext): CompletableFuture<Any?> {
        val structured = actionSpec
        if (structured == null) {
            return CompletableFuture.completedFuture(null)
        }
        val task = {
            runStructuredFlow(structured, ActionSpec.FIRE, execution)
            null
        }
        if (async) {
            return CompletableFuture.supplyAsync(task)
        }
        return CompletableFuture.completedFuture(task())
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

    override fun toString(): String = "ImmutableSkill(id='$id', startedLevel=$startedLevel, immutableVariables=$immutableVariables)"

}
