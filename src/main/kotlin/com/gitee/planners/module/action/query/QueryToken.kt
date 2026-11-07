package com.gitee.planners.module.action.query

/**
 * 求值表达式的词法单元。
 *
 * @property type 单元类型。
 * @property text 原始文本。
 * @property number 数字字面量的值；非数字为 null。
 * @property position 在源文本中的起始下标，用于错误定位。
 */
class QueryToken(
    val type: QueryTokenType,
    val text: String,
    val number: Double? = null,
    val position: Int = 0,
) {

    /**
     * 判断是否为指定类型。
     *
     * @param expected 期望类型。
     * @return 类型一致时返回 true。
     */
    fun isType(expected: QueryTokenType): Boolean {
        return type == expected
    }

    override fun toString(): String {
        return type.name + "(" + text + ")"
    }
}
