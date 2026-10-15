package com.gitee.planners.core.condition

import com.gitee.planners.module.compat.haidercore.WeaponMasteryCondition
import taboolib.library.configuration.ConfigurationSection

/** 根据模板类型创建条件，业务实现分别归属 Nova 和外部插件适配包。 */
object ConditionFactory {
    /** 解码一个条件模板；未声明 type 的脚本模板沿用 Nova 本身的配置语义。 */
    fun create(key: String, section: ConfigurationSection): ConditionConfig {
        val props = LinkedHashMap<String, Any>()
        val properties = section.getConfigurationSection("props")
        if (properties != null) {
            for ((name, value) in properties.getValues(false)) {
                require(value != null) { "条件 $key 的属性 $name 不能为空" }
                props[name] = value
            }
        }
        val type = section.getString("type", "nova")
        if (type == "weapon_mastery") {
            require(!section.contains("exper") && !section.contains("consume")) { "武器熟练度条件不接受脚本或消耗" }
            val hint = section.getString("hint", "需要武器 {props.weapon} 熟练度 Lv{props.min}")!!
            return WeaponMasteryCondition(key, props, hint)
        }
        require(type == "nova") { "未知条件类型：$type" }
        val expression = section.getString("exper")
        val hint = section.getString("hint")
        require(expression != null && hint != null) { "Nova 条件 $key 缺少 exper 或 hint" }
        val consumeText = section.getString("consume")
        val consume = if (consumeText.isNullOrEmpty()) {
            null
        } else {
            consumeText
        }
        return NovaConditionConfig(key, expression, props, hint, consume)
    }
}
