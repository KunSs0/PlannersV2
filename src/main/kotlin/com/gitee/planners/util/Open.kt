package com.gitee.planners.util

import org.bukkit.Bukkit

/**
 * 检查插件是否存在
 *
 * @param name 插件名称
 *
 * @return 是否存在
 */
fun checkPlugin(name: String): Boolean {
    return Bukkit.getPluginManager().getPlugin(name) != null
}

/**
 * 检查插件类是否存在
 *
 * @param clazz 插件类
 *
 * @return 是否存在
 */
fun checkPluginClass(clazz: String): Boolean {
    return try {
        Class.forName(clazz)
        true
    } catch (ex: ClassNotFoundException) {
        false
    }
}