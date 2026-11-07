package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryRef
import com.gitee.planners.module.action.query.QueryScope
import com.gitee.planners.module.action.query.QueryText
import taboolib.library.configuration.ConfigurationSection

/**
 * 属性表达式的编译与求值。
 *
 * `hook.attributes` 的每个值都是一段查询表达式：职业用 `profile.*`，
 * 技能用 `skill.*`。求值结果不是数值时按「该属性未提供」处理，由调用方跳过，
 * 不影响其他属性。
 */
object AttributeExpressions {

    /**
     * 编译一段属性表达式。
     *
     * 数字直接作为常量；文本按查询引擎判定为路径或公式。
     *
     * @param raw 配置中的原始值。
     * @param label 配置路径，用于错误信息。
     * @return 已编译的取值引用。
     */
    fun compile(raw: Any?, label: String): QueryRef {
        if (raw is Number) {
            val text = QueryText.renderValue(raw.toDouble())
            return QueryRef.parse(text, label)
        }
        if (raw is String) {
            return QueryRef.parse(raw, label)
        }
        throw IllegalArgumentException("$label must be a number or an expression.")
    }

    /**
     * 编译整段 `hook.attributes`。
     *
     * @param root 父节点。
     * @param labelPrefix 配置路径前缀。
     * @return 属性名到取值引用的映射；未配置时返回空映射。
     */
    fun compileSection(root: ConfigurationSection, labelPrefix: String): Map<String, QueryRef> {
        val section = root.getConfigurationSection("hook.attributes")
        if (section == null) {
            return emptyMap()
        }
        val result = LinkedHashMap<String, QueryRef>()
        for (key in section.getKeys(false)) {
            result[key] = compile(section.get(key), "$labelPrefix.hook.attributes.$key")
        }
        return result
    }

    /**
     * 求值一个属性。
     *
     * @param reference 已编译的取值引用。
     * @param scope 查询上下文。
     * @return 数值；结果缺失或不是有限数值时返回 null。
     */
    fun evaluate(reference: QueryRef, scope: QueryScope): Double? {
        val value = reference.resolve(scope)
        val number = QueryText.numberOrNull(value)
        if (number == null || !number.isFinite()) {
            return null
        }
        return number
    }
}
