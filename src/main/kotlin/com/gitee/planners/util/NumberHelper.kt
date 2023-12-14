package com.gitee.planners.util

/**
 * 基本数值类型拆箱
 */
fun unboxJavaToKotlin(clazz: Class<*>): Class<*> {

    return when (clazz) {
        Integer.TYPE -> Integer::class.java
        java.lang.Long.TYPE -> java.lang.Long::class.java
        java.lang.Float.TYPE -> java.lang.Float::class.java
        java.lang.Double.TYPE -> java.lang.Double::class.java
        else -> clazz
    }
}
