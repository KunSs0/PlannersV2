package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope

/**
 * 一条已编译的条件。
 *
 * @property condition 条件实现。
 * @property arguments 条件参数。
 * @property expected 期望结果；false 表示取反。
 * @property path 配置路径。
 * @property compiled 条件实现在加载期产生的缓存对象。
 */
class ActionConditionEntry(
    val condition: ActionCondition,
    val arguments: ActionArguments,
    val expected: Boolean,
    val path: String,
    private val compiled: Any?,
) {

    /**
     * 使用当前上下文求值。
     *
     * @param scope 查询上下文。
     * @return 条件结果。
     */
    fun matches(scope: QueryScope): Boolean {
        val result = condition.test(scope, arguments, compiled)
        if (expected) {
            return result
        }
        return !result
    }

    /**
     * 返回条件的稳定描述，用于诊断输出。
     *
     * @return 形如 `node{...}` 的文本。
     */
    fun describe(): String {
        val builder = StringBuilder()
        builder.append(condition.id)
        val values = arguments.all()
        if (values.isNotEmpty()) {
            builder.append("{")
            var first = true
            for (key in values.keys) {
                if (!first) {
                    builder.append("; ")
                }
                builder.append(key)
                first = false
            }
            builder.append("}")
        }
        if (!expected) {
            builder.append(" false")
        }
        return builder.toString()
    }
}
