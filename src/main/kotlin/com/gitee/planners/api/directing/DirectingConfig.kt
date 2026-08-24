package com.gitee.planners.api.directing

/**
 * 指向性技能配置读取契约。
 *
 * 该接口隔离底层配置库，避免 provider API 暴露会被插件 API 打包重定位的配置类型。
 */
interface DirectingConfig {

    /**
     * 判断配置是否包含指定字段。
     *
     * @param key 配置字段名。
     * @return true 表示字段存在。
     */
    fun contains(key: String): Boolean

    /**
     * 读取整数配置。
     *
     * @param key 配置字段名。
     * @return 字段对应的整数值。
     */
    fun getInt(key: String): Int

    /**
     * 读取小数配置。
     *
     * @param key 配置字段名。
     * @return 字段对应的小数值。
     */
    fun getDouble(key: String): Double

    /**
     * 读取布尔配置。
     *
     * @param key 配置字段名。
     * @return 字段对应的布尔值。
     */
    fun getBoolean(key: String): Boolean

    /**
     * 读取字符串配置。
     *
     * @param key 配置字段名。
     * @return 字段对应的字符串；字段不存在时返回 null。
     */
    fun getString(key: String): String?
}
