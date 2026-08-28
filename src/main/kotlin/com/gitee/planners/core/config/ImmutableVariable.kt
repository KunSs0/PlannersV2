package com.gitee.planners.core.config

import com.gitee.planners.api.job.Variable
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration

/** 启动期并入技能变量批处理函数的不可变变量定义。 */
interface ImmutableVariable : Variable {

    /** 向技能变量批处理函数写入局部计算语句。 */
    fun appendDisplayEvaluation(builder: StringBuilder)

    companion object {

        private val identifierPattern = Regex("^[A-Za-z_][A-Za-z0-9_]*$")

        fun parse(id: String, value: Any): ImmutableVariable {
            require(identifierPattern.matches(id)) {
                "Variable ID '$id' must be a valid Nova identifier"
            }
            return when (value) {
                is String -> Default(id, value)
                is Boolean, is Int, is Float, is Double, is Long -> Default(id, "$value")
                is List<*> -> {
                    val first = value.firstOrNull()
                    if (first is Map<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        When(id, value.map { Configuration.fromMap(it as Map<*, *>) })
                    } else {
                        Default(id, value.joinToString())
                    }
                }
                else -> error("Unsupported value type ${value::class.java}")
            }
        }
    }

    /** 普通 Nova 变量表达式。 */
    class Default(override val id: String, private val expression: String) : ImmutableVariable {

        override fun appendDisplayEvaluation(builder: StringBuilder) {
            builder.append("val ")
            builder.append(id)
            builder.append(" = (")
            builder.append(expression)
            builder.append(")\n")
        }
    }

    /** 按声明顺序匹配的 Nova 条件变量表达式。 */
    class When(override val id: String, values: List<ConfigurationSection>) : ImmutableVariable {

        private val expression = createExpression(values)

        override fun appendDisplayEvaluation(builder: StringBuilder) {
            builder.append("val ")
            builder.append(id)
            builder.append(" = (")
            builder.append(expression)
            builder.append(")\n")
        }

        private fun createExpression(values: List<ConfigurationSection>): String {
            if (values.isEmpty()) {
                throw IllegalArgumentException("Variable '$id' must declare at least one condition branch")
            }
            var generated = "false"
            for (index in values.size - 1 downTo 0) {
                val section = values[index]
                val condition = section.getString("condition")
                if (condition == null || condition.isBlank()) {
                    throw IllegalArgumentException("Variable '$id' branch $index is missing condition")
                }
                val action = section.getString("action")
                if (action == null || action.isBlank()) {
                    throw IllegalArgumentException("Variable '$id' branch $index is missing action")
                }
                generated = "if ($condition) ($action) else ($generated)"
            }
            return generated
        }
    }
}
