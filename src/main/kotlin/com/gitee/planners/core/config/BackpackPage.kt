package com.gitee.planners.core.config

import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.util.mapSection

/**
 * 技能背包页面配置。
 *
 * @param config 页面配置节点。
 * @param categorySpecs 技能分类规格。
 * @property id 页面 ID。
 * @property name 页面显示名称。
 * @property slots 页面槽位。
 */
class BackpackPage(config: ConfigurationSection, categorySpecs: Map<String, SkillCategorySpec>) {

    val id: String = config.name

    val name: String = config.getString("name", id)!!

    val slots: Map<String, BackpackSlot> =
        config.getConfigurationSection("slots")?.mapSection { BackpackSlot(it, categorySpecs) } ?: emptyMap()

    /**
     * 按按键 ID 获取槽位。
     *
     * @param keyId 按键 ID。
     * @return 对应槽位，不存在时返回 null。
     */
    fun getSlotForKey(keyId: String): BackpackSlot? {
        for (slot in slots.values) {
            if (slot.key == keyId) {
                return slot
            }
        }
        return null
    }
}
