package com.gitee.planners.core.config

/**
 * 技能分类解析与匹配工具。
 */
object SkillCategories {

    /**
     * 全部分类通配符。
     */
    const val WILDCARD = "*"

    /**
     * 将配置中的单值或列表解析为分类集合。
     *
     * @param value 配置值。
     * @param source 配置来源描述。
     * @return 去重后的分类集合。
     * @throws IllegalArgumentException 配置缺失、为空或包含空分类时抛出。
     */
    fun parse(value: Any?, source: String): Set<String> {
        if (value == null) {
            throw IllegalArgumentException("$source 缺少 category 配置")
        }

        val result = linkedSetOf<String>()
        if (value is Iterable<*>) {
            for (item in value) {
                addCategory(result, item, source)
            }
        } else {
            addCategory(result, value, source)
        }

        if (result.isEmpty()) {
            throw IllegalArgumentException("$source 的 category 不能为空")
        }
        return result
    }

    /**
     * 校验分类是否都已在规格中声明。
     *
     * @param categories 待校验分类。
     * @param specs 分类规格。
     * @param source 配置来源描述。
     * @throws IllegalArgumentException 存在未声明分类时抛出。
     */
    fun validate(categories: Set<String>, specs: Map<String, SkillCategorySpec>, source: String) {
        for (category in categories) {
            if (category != WILDCARD && !specs.containsKey(category)) {
                throw IllegalArgumentException("$source 引用了未声明的技能分类 '$category'")
            }
        }
    }

    /**
     * 判断技能分类是否满足槽位分类限制。
     *
     * @param skillCategories 技能分类。
     * @param slotCategories 槽位允许分类。
     * @return 存在通配符或分类交集时返回 true。
     */
    fun matches(skillCategories: Set<String>, slotCategories: Set<String>): Boolean {
        if (skillCategories.contains(WILDCARD) || slotCategories.contains(WILDCARD)) {
            return true
        }
        for (category in skillCategories) {
            if (slotCategories.contains(category)) {
                return true
            }
        }
        return false
    }

    /**
     * 添加并校验单个分类值。
     *
     * @param result 分类结果集合。
     * @param value 待添加值。
     * @param source 配置来源描述。
     * @throws IllegalArgumentException 值为空或为空字符串时抛出。
     */
    private fun addCategory(result: MutableSet<String>, value: Any?, source: String) {
        if (value == null) {
            throw IllegalArgumentException("$source 的 category 包含空值")
        }
        val category = value.toString().trim()
        if (category.isEmpty()) {
            throw IllegalArgumentException("$source 的 category 包含空字符串")
        }
        result.add(category)
    }
}
