package com.gitee.planners.core.config

import com.gitee.planners.api.Registries
import com.gitee.planners.api.job.Variable
import com.gitee.planners.module.action.ActionScopeFactory
import com.gitee.planners.module.action.query.QueryScope
import com.gitee.planners.util.getOption
import com.gitee.planners.util.mapValueWithId
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.xseries.getItemStack
import taboolib.module.configuration.Configuration
import java.util.Locale

class ImmutableJob(private val config: Configuration) {

    val id = config.file!!.nameWithoutExtension

    private val option = config.getOption()

    val name = option.getString("name", id)!!

    /** 职业显示图标的 namespaced ID；未配置时为空。 */
    val iconItemId: String? = toItemId(option.getString("display.icon.material"))

    /** 职业显示图标名称的展示文本；`<...>` 按求值表达式解析。 */
    val displayIconName: DisplayText? = DisplayText.compileOrNull(
        option.getString("display.icon.name"),
        "job:$id.display.icon.name",
    )

    /** 职业显示图标 Lore 的展示文本，逐行编译。 */
    val displayIconLore: List<DisplayText> = DisplayText.compileList(
        option.getStringList("display.icon.lore"),
        "job:$id.display.icon.lore",
    )

    /** 职业名称的展示文本。 */
    val nameText: DisplayText = DisplayText.compile(name, "job:$id.name")

    /**
     * 职业 Bukkit 显示图标；未配置时为空。
     *
     * 仅在传统 Bukkit GUI 读取时解码为 ItemStack。
     */
    val icon: ItemStack?
        get() {
            val display = option.getConfigurationSection("display")
            if (display == null) {
                return null
            }
            return display.getItemStack("icon")
        }

    val immutableVariables = option.mapValueWithId("variables") { id: String, value: Any ->
        ImmutableVariable.parse(id, value)
    }

    val skillIds = option.getStringList("skill")

    init {
        val seenSkills = mutableSetOf<String>()
        val duplicateSkills = mutableSetOf<String>()
        for (skillId in skillIds) {
            if (!seenSkills.add(skillId)) {
                duplicateSkills.add(skillId)
            }
        }
        if (duplicateSkills.isNotEmpty()) {
            error("Job '$id' contains duplicate skills: ${duplicateSkills.joinToString(", ")}")
        }
    }

    /**
     * 职业提供的属性。
     * key = 物理属性 ID（由当前属性驱动定义）
     * value = 查询表达式，例如 `10 + (profile.level - 1) * 1`
     */
    val attributeExpressions: Map<String, com.gitee.planners.module.action.query.QueryRef> =
        com.gitee.planners.module.action.AttributeExpressions.compileSection(option, "job:$id")

    fun hasSkill(id: String): Boolean {
        return skillIds.contains(id)
    }

    /**
     * 渲染职业的全部展示文本。
     *
     * 名称、图标名称与 Lore 共用同一个查询上下文，变量只计算一次；
     * 三者都不含占位符时直接返回原文，不构造上下文。
     *
     * @param player 当前玩家。
     * @return 已渲染的名称、图标名称与 Lore。
     */
    fun renderDisplay(player: Player): DisplayTexts {
        val nameSource = nameText
        val iconNameSource = displayIconName
        val dynamicName = nameSource.dynamic
        val dynamicIconName = iconNameSource != null && iconNameSource.dynamic
        val dynamicLore = hasDynamicLore()
        if (!dynamicName && !dynamicIconName && !dynamicLore) {
            val rawIconName = if (iconNameSource == null) null else iconNameSource.raw
            return DisplayTexts(nameSource.raw, rawIconName, staticLoreLines())
        }
        val scope = displayScope(player)
        val name = if (dynamicName) {
            nameSource.render(scope)
        } else {
            nameSource.raw
        }
        val iconName = if (dynamicIconName) {
            iconNameSource?.render(scope)
        } else {
            if (iconNameSource == null) null else iconNameSource.raw
        }
        return DisplayTexts(name, iconName, renderLore(scope))
    }

    /**
     * 渲染职业图标名称。
     *
     * @param scope 本次请求共用的查询上下文。
     * @return 渲染结果；未配置图标名称时返回 null。
     */
    fun renderDisplayIconName(scope: QueryScope): String? {
        val text = displayIconName
        if (text == null) {
            return null
        }
        if (!text.dynamic) {
            return text.raw
        }
        return text.render(scope)
    }

    /**
     * 渲染职业图标 Lore。
     *
     * @param scope 本次请求共用的查询上下文。
     * @return 逐行渲染结果，顺序与配置一致。
     */
    fun renderDisplayIconLore(scope: QueryScope): List<String> {
        return renderLore(scope)
    }

    /**
     * 渲染职业名称。
     *
     * @param scope 本次请求共用的查询上下文。
     * @return 渲染结果；名称不含占位符时直接返回原文。
     */
    fun renderName(scope: QueryScope): String {
        if (!nameText.dynamic) {
            return nameText.raw
        }
        return nameText.render(scope)
    }

    /**
     * 渲染职业名称。
     *
     * 名称不含占位符时直接返回原文，不构造查询上下文。
     *
     * @param player 当前玩家。
     * @return 渲染结果。
     */
    fun renderName(player: Player): String {
        if (!nameText.dynamic) {
            return nameText.raw
        }
        return nameText.render(displayScope(player))
    }

    /**
     * 构造职业展示渲染用的查询上下文。
     *
     * 职业没有技能等级，因此只绑定玩家档案与职业自身变量；
     * 变量按声明顺序求值，后声明的可以引用先声明的结果。
     *
     * @param player 当前玩家。
     * @return 已完成变量计算的查询上下文。
     */
    fun displayScope(player: Player): QueryScope {
        val variables = LinkedHashMap<String, Any?>()
        val scope = ActionScopeFactory.forPlayer(player, null, 0, variables, null, "job:$id:display")
        for (entry in immutableVariables) {
            variables[entry.key] = entry.value.evaluate(scope)
        }
        return scope
    }

    /**
     * 判断 Lore 是否含占位符。
     *
     * @return 含占位符时返回 true。
     */
    private fun hasDynamicLore(): Boolean {
        for (text in displayIconLore) {
            if (text.dynamic) {
                return true
            }
        }
        return false
    }

    /**
     * 返回未插值的 Lore 原文。
     *
     * @return 原文列表。
     */
    private fun staticLoreLines(): List<String> {
        val result = ArrayList<String>(displayIconLore.size)
        for (text in displayIconLore) {
            result.add(text.raw)
        }
        return result
    }

    /**
     * 逐行渲染 Lore。
     *
     * @param scope 本次请求共用的查询上下文。
     * @return 渲染结果。
     */
    private fun renderLore(scope: QueryScope): List<String> {
        val result = ArrayList<String>(displayIconLore.size)
        for (text in displayIconLore) {
            if (!text.dynamic) {
                result.add(text.raw)
                continue
            }
            result.add(text.render(scope))
        }
        return result
    }

    /**
     * 职业展示文本。
     *
     * @property name 已渲染的职业名称。
     * @property iconName 已渲染的图标名称；未配置时为 null。
     * @property lore 已渲染的图标 Lore。
     */
    class DisplayTexts(val name: String, val iconName: String?, val lore: List<String>)

    fun getImmutableSkillValues(): List<ImmutableSkill> {
        return Registries.SKILL.values().filter { it.id in skillIds }
    }

    fun getSkillOrNull(id: String): ImmutableSkill? {
        if (!hasSkill(id)) {
            return null
        }
        return Registries.SKILL.getOrNull(id)
    }

    fun getVariableOrNull(id: String): Variable? {
        return immutableVariables[id]
    }

    fun getVariables(): Map<String, Variable> {
        return immutableVariables
    }

    /**
     * 将职业图标材质文本转换为 Minecraft namespaced ID。
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
}
