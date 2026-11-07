package com.gitee.planners.module.action.query

/**
 * 查询结果的文本渲染与类型转换。
 *
 * 渲染规则与 Creator 的插值规范保持一致：整数不带小数位，
 * 缺失值在文本场景返回空串、在数值场景报配置错误。
 */
object QueryText {

    /**
     * 判断文本是否被 `<>` 外壳包裹。
     *
     * @param text 已去空白的文本。
     * @return 首尾分别为 `<` 与 `>` 时返回 true。
     */
    fun isWrapped(text: String): Boolean {
        if (text.length < 2) {
            return false
        }
        return text.startsWith("<") && text.endsWith(">")
    }

    /**
     * 剥离 `<>` 外壳。
     *
     * @param text 已确认被包裹的文本。
     * @param label 配置路径，用于错误信息。
     * @return 内部文本。
     * @throws IllegalArgumentException 内部为空时抛出。
     */
    fun unwrap(text: String, label: String): String {
        val inner = text.substring(1, text.length - 1).trim()
        if (inner.isEmpty()) {
            throw IllegalArgumentException("$label contains an empty '<>' query.")
        }
        return inner
    }

    /**
     * 解析一个字面量标量。
     *
     * 支持整数、浮点、布尔与带引号字符串；其余按原样文本返回。
     *
     * @param text 已去空白的文本。
     * @return 解析结果。
     */
    fun parseScalar(text: String): Any? {
        if (text.equals("true", true)) {
            return true
        }
        if (text.equals("false", true)) {
            return false
        }
        if (text.equals("null", true)) {
            return null
        }
        val integer = text.toLongOrNull()
        if (integer != null) {
            return integer
        }
        val decimal = text.toDoubleOrNull()
        if (decimal != null) {
            return decimal
        }
        return stripQuotes(text)
    }

    /**
     * 去掉成对的单引号或双引号。
     *
     * @param text 原始文本。
     * @return 去引号后的文本。
     */
    fun stripQuotes(text: String): String {
        if (text.length < 2) {
            return text
        }
        val first = text[0]
        val last = text[text.length - 1]
        if (first != last) {
            return text
        }
        if (first != '"' && first != '\'') {
            return text
        }
        return text.substring(1, text.length - 1)
    }

    /**
     * 尝试把值读成数值。
     *
     * 与 [requireDouble] 的区别是读不到时返回 null 而不是抛错，供比较运算与下标取值使用。
     * 字符串按内容解析，因此元数据里的 `"1"` 与数字 `1` 可以相等。
     *
     * @param value 待转换值。
     * @return 数值；无法转换时返回 null。
     */
    fun numberOrNull(value: Any?): Double? {
        if (value == null) {
            return null
        }
        if (value is Number) {
            val result = value.toDouble()
            if (!result.isFinite()) {
                return null
            }
            return result
        }
        if (value is String) {
            return value.trim().toDoubleOrNull()
        }
        return null
    }

    /**
     * 尝试把值读成布尔。
     *
     * @param value 待转换值。
     * @return 布尔；无法转换时返回 null。
     */
    fun booleanOrNull(value: Any?): Boolean? {
        if (value is Boolean) {
            return value
        }
        if (value is String) {
            if (value.equals("true", true)) {
                return true
            }
            if (value.equals("false", true)) {
                return false
            }
        }
        return null
    }

    /**
     * 把求值结果渲染为文本。
     *
     * @param value 求值结果。
     * @return 文本；null 返回空串。
     */
    fun renderValue(value: Any?): String {
        if (value == null) {
            return ""
        }
        if (value is Double) {
            return renderDouble(value)
        }
        if (value is Float) {
            return renderDouble(value.toDouble())
        }
        return value.toString()
    }

    /**
     * 渲染浮点数，整数结果不带小数位。
     *
     * @param value 浮点值。
     * @return 文本。
     */
    private fun renderDouble(value: Double): String {
        if (!value.isFinite()) {
            return value.toString()
        }
        if (value == Math.floor(value) && Math.abs(value) < MAX_EXACT_INTEGRAL) {
            return value.toLong().toString()
        }
        return value.toString()
    }

    /**
     * 把求值结果转换为数值。
     *
     * @param value 求值结果。
     * @param label 配置路径，用于错误信息。
     * @return 数值。
     * @throws IllegalArgumentException 结果不可转换时抛出。
     */
    fun requireDouble(value: Any?, label: String): Double {
        if (value is Number) {
            return value.toDouble()
        }
        if (value is Boolean) {
            return if (value) {
                1.0
            } else {
                0.0
            }
        }
        if (value is String) {
            val parsed = value.trim().toDoubleOrNull()
            if (parsed != null) {
                return parsed
            }
        }
        throw IllegalArgumentException("$label requires a number, got: ${describe(value)}")
    }

    /**
     * 把求值结果转换为布尔。
     *
     * @param value 求值结果。
     * @param label 配置路径，用于错误信息。
     * @return 布尔值。
     * @throws IllegalArgumentException 结果不可转换时抛出。
     */
    fun requireBoolean(value: Any?, label: String): Boolean {
        if (value is Boolean) {
            return value
        }
        throw IllegalArgumentException("$label requires a boolean, got: ${describe(value)}")
    }

    /**
     * 描述一个值的类型与内容，用于错误信息。
     *
     * @param value 待描述的值。
     * @return 描述文本。
     */
    fun describe(value: Any?): String {
        if (value == null) {
            return "null"
        }
        return value::class.java.simpleName + "(" + value + ")"
    }

    /** 浮点渲染时仍按整数输出的上限，避免大数丢精度。 */
    private const val MAX_EXACT_INTEGRAL = 1.0E15
}
