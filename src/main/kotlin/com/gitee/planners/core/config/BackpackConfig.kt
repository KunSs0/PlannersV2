package com.gitee.planners.core.config

import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.util.mapSection

/**
 * 技能背包配置。
 *
 * @param config 背包配置节点。
 * @param categorySpecs 技能分类规格。
 * @property defaultPage 默认页面 ID。
 * @property pages 背包页面。
 */
class BackpackConfig(config: ConfigurationSection, categorySpecs: Map<String, SkillCategorySpec>) {

    val defaultPage: String = config.getString("default-page", "0")!!

    val pages: Map<String, BackpackPage> =
        config.getConfigurationSection("pages")?.mapSection { BackpackPage(it, categorySpecs) } ?: emptyMap()

    /**
     * 按 ID 获取背包页面。
     *
     * @param id 页面 ID。
     * @return 页面配置，不存在时返回 null。
     */
    fun getPage(id: String): BackpackPage? {
        return pages[id]
    }

    /**
     * 获取第一个页面 ID。
     *
     * @return 第一个页面 ID，不存在页面时返回 null。
     */
    fun getFirstPageId(): String? {
        for (id in pages.keys) {
            return id
        }
        return null
    }
}
