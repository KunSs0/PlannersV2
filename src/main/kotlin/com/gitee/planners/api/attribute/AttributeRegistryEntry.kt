package com.gitee.planners.api.attribute

/**
 * 属性注册项：定义逻辑属性到物理属性的转换规则。
 *
 * @param key      逻辑属性键，如 "STR"
 * @param name     展示名，如 "力量"
 * @param mappings 物理属性键 → 转换系数
 */
data class AttributeRegistryEntry(
    val key: String,
    val name: String,
    val mappings: Map<String, Double>
)
