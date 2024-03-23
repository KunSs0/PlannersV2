package com.gitee.planners.module.compat

fun isPluginHooked(clazz: String): Boolean {
    return try {
        Class.forName(clazz)
        true
    } catch (e: ClassNotFoundException) {
        false
    }
}
