package com.gitee.planners.core.condition

import com.gitee.planners.module.compat.haidercore.WeaponMasteryCondition
import taboolib.library.configuration.ConfigurationSection

/** 根据模板类型创建条件，业务实现分别归属 Nova 和外部插件适配包。 */
object ConditionFactory {
    /** 解码一个条件模板；学习条件必须声明由 Planners 提供的固定 type。 */
    fun create(key: String, section: ConfigurationSection): ConditionConfig {
        val props = LinkedHashMap<String, Any>()
        val properties = section.getConfigurationSection("props")
        if (properties != null) {
            for ((name, value) in properties.getValues(false)) {
                require(value != null) { "条件 $key 的属性 $name 不能为空" }
                props[name] = value
            }
        }
        val configuredType = section.getString("type")
        require(configuredType != null) { "条件 $key 的 type 不能为空" }
        val type = configuredType
        if (type == "weapon_mastery") {
            require(!section.contains("exper") && !section.contains("consume")) { "武器熟练度条件不接受脚本或消耗" }
            val hint = section.getString("hint", "需要武器 {props.weapon} 熟练度 Lv{props.min}")!!
            return WeaponMasteryCondition(key, props, hint)
        }
        val hint = section.getString("hint")
        require(hint != null) { "原生条件 $key 缺少 hint" }
        require(!section.contains("exper") && !section.contains("prepare")) { "条件 $key 不支持 Nova exper/prepare" }
        val consumeValue = section["consume"]
        require(consumeValue == null || consumeValue is Boolean) { "条件 $key 的 consume 必须是 Boolean" }
        val consume = section.getBoolean("consume", false)
        val config = NativeConditionConfig(key, type, props, hint, consume)
        config.validateProperties(props)
        return config
    }
}
