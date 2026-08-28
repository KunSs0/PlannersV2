package com.gitee.planners.core.config

import com.gitee.planners.api.Registries
import com.gitee.planners.api.job.Variable
import com.gitee.planners.module.script.SingletonScript
import com.gitee.planners.util.getOption
import com.gitee.planners.util.mapValueWithId
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

    /** 职业显示图标名称原文。 */
    val displayIconName: String? = option.getString("display.icon.name")

    /** 职业显示图标 Lore 原文。 */
    val displayIconLore: List<String> = option.getStringList("display.icon.lore")

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

    /** 启动期预编译的职业属性 Nova 表达式。 */
    val attributeScripts: Map<String, SingletonScript> = attributes.mapValues { entry ->
        SingletonScript(entry.value, "job:$id:attribute:${entry.key}", listOf("sender", "profile", "level"))
    }

    fun hasSkill(id: String): Boolean {
        return skillIds.contains(id)
    }

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
