package com.gitee.planners.core.skill.directing

import com.gitee.planners.api.directing.DirectingConfig
import taboolib.library.configuration.ConfigurationSection

/**
 * TabooLib 配置段到 [DirectingConfig] 的内部适配器。
 *
 * 该类只存在于 Planners 本体，使公开 provider API 不泄漏被 API 打包重定位的 TabooLib 类型。
 *
 * @property section 待读取的 directing 配置段。
 */
class DirectingConfigSection(private val section: ConfigurationSection) : DirectingConfig {

    /**
     * 判断字段是否存在。
     */
    override fun contains(key: String): Boolean {
        return section.contains(key)
    }

    /**
     * 读取整数值。
     */
    override fun getInt(key: String): Int {
        return section.getInt(key)
    }

    /**
     * 读取小数值。
     */
    override fun getDouble(key: String): Double {
        return section.getDouble(key)
    }

    /**
     * 读取布尔值。
     */
    override fun getBoolean(key: String): Boolean {
        return section.getBoolean(key)
    }

    /**
     * 读取字符串值。
     */
    override fun getString(key: String): String? {
        return section.getString(key)
    }
}
