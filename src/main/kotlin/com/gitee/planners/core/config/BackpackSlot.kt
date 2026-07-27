package com.gitee.planners.core.config

import com.gitee.planners.core.player.PlayerSkill
import taboolib.library.configuration.ConfigurationSection

/**
 * 技能背包槽位配置。
 *
 * @param config 槽位配置节点。
 * @param categorySpecs 技能分类规格。
 * @property id 槽位 ID。
 * @property key 槽位绑定的按键 ID。
 * @property categories 槽位允许的技能分类。
 */
class BackpackSlot(config: ConfigurationSection, categorySpecs: Map<String, SkillCategorySpec>) {

    val id: String = config.name

    val key: String = config.getString("key")!!

    val categories: Set<String>
    init {
        val categoryValue = config["category"]
        categories = SkillCategories.parse(categoryValue, "背包槽位 '$id'")
        SkillCategories.validate(categories, categorySpecs, "背包槽位 '$id'")
    }

    /**
     * 判断技能是否可以装备到该槽位。
     *
     * @param skill 待装备技能。
     * @return 分类匹配时返回 true。
     */
    fun accepts(skill: PlayerSkill): Boolean {
        return SkillCategories.matches(skill.immutable.categories, categories)
    }
}
