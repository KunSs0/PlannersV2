package com.gitee.planners.core.config

import com.gitee.planners.api.job.Variable
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration

/**
 * 技能变量定义：普通值通过参数绑定，只有显式 expression 和条件分支参与编译。
 */
interface ImmutableVariable : Variable {
    /**
     * 按配置顺序声明变量，常量只读取宿主参数，不把配置文本作为代码。
     *
     * @param builder 技能变量批处理函数正文。
     */
    fun appendDisplayEvaluation(builder: StringBuilder)

    companion object {
        const val LITERALS_PARAMETER = "__plannersVariableLiterals"
        private val identifierPattern = Regex("^[A-Za-z_][A-Za-z0-9_]*$")

        /**
         * 解析配置，字符串与基本类型直接作为常量，映射必须显式声明表达式。
         *
         * @param id 上下文变量名。
         * @param value YAML 中的原始值。
         * @return 常量、显式表达式或已有的条件分支定义。
         * @throws IllegalArgumentException 名称或表达式结构不合法时抛出。
         */
        fun parse(id: String, value: Any): ImmutableVariable {
            require(identifierPattern.matches(id) && id != LITERALS_PARAMETER) {
                "变量 '$id' 必须是合法且非保留的 Nova 标识符。"
            }
            if (value is ConfigurationSection) {
                return parseExpression(id, value.getValues(false))
            }
            if (value is Map<*, *>) {
                return parseExpression(id, value)
            }
            if (value is List<*>) {
                val first = value.firstOrNull()
                if (first is Map<*, *>) {
                    val branches = ArrayList<ConfigurationSection>()
                    for (branch in value) {
                        require(branch is Map<*, *>) { "变量 '$id' 的条件分支必须全部为映射。" }
                        branches.add(Configuration.fromMap(branch))
                    }
                    return When(id, branches)
                }
                return Literal(id, copyLiteralList(value))
            }
            require(value is String || value is Boolean || value is Number) {
                "变量 '$id' 不支持配置类型 ${value::class.java.name}。"
            }
            return Literal(id, value)
        }

        /**
         * 提取原始常量绑定，不执行解析、插值或求值。
         *
         * @param variables 已解析的有序变量定义。
         * @return 按名称保存原始常量值的绑定。
         */
        fun literalBindings(variables: Map<String, ImmutableVariable>): Map<String, Any> {
            val result = LinkedHashMap<String, Any>()
            for ((id, variable) in variables) {
                if (variable is Literal) {
                    val value = variable.value
                    if (value is List<*>) {
                        result[id] = copyLiteralList(value)
                    } else {
                        result[id] = value
                    }
                }
            }
            return result
        }

        /**
         * 为每次求值复制列表，避免 action 修改数据后污染下一次施法的配置。
         *
         * @param values 原始常量列表。
         * @return 独立的递归列表快照。
         * @throws IllegalArgumentException 列表包含不支持的对象类型时抛出。
         */
        private fun copyLiteralList(values: List<*>): List<Any?> {
            val copy = ArrayList<Any?>()
            for (value in values) {
                if (value is List<*>) {
                    copy.add(copyLiteralList(value))
                } else {
                    require(value == null || value is String || value is Boolean || value is Number) {
                        "变量常量列表只能包含字符串、数字、布尔、null 或嵌套列表。"
                    }
                    copy.add(value)
                }
            }
            return copy
        }

        /**
         * 解析唯一的显式表达式字段，拒绝拼写错误与额外字段。
         *
         * @param id 变量名。
         * @param fields 表达式结构。
         * @return 预编译表达式定义。
         * @throws IllegalArgumentException 字段不完整、为空或类型不正确时抛出。
         */
        private fun parseExpression(id: String, fields: Map<*, *>): ImmutableVariable {
            require(fields.size == 1 && fields.containsKey("expression")) {
                "变量 '$id' 的映射只允许 expression 字段。"
            }
            val expression = fields["expression"]
            require(expression is String && expression.isNotBlank()) {
                "变量 '$id.expression' 必须是非空 Nova 表达式字符串。"
            }
            return Expression(id, expression)
        }
    }

    /**
     * 不参与代码编译的原始常量。
     *
     * @property id 变量名。
     * @property value YAML 原始常量值。
     */
    class Literal(override val id: String, val value: Any) : ImmutableVariable {
        /**
         * 从宿主参数读取常量，保留字符串中的引号、换行与插值符号。
         *
         * @param builder 变量批处理函数正文。
         */
        override fun appendDisplayEvaluation(builder: StringBuilder) {
            builder.append("val ")
            builder.append(id)
            builder.append(" = ")
            builder.append(LITERALS_PARAMETER)
            builder.append("[\"")
            builder.append(id)
            builder.append("\"]\n")
        }
    }

    /**
     * 通过 expression 字段显式声明的计算表达式。
     *
     * @property id 变量名。
     * @property expression 待预编译的 Nova 表达式。
     */
    class Expression(override val id: String, private val expression: String) : ImmutableVariable {
        /**
         * 保留表达式的原始计算语义与声明顺序。
         *
         * @param builder 变量批处理函数正文。
         */
        override fun appendDisplayEvaluation(builder: StringBuilder) {
            builder.append("val ")
            builder.append(id)
            builder.append(" = (")
            builder.append(expression)
            builder.append(")\n")
        }
    }

    /**
     * 按声明顺序匹配的条件变量；condition 与 action 本身就是显式表达式字段。
     *
     * @property id 变量名。
     * @param values 条件与结果表达式列表。
     */
    class When(override val id: String, values: List<ConfigurationSection>) : ImmutableVariable {
        private val expression = createExpression(values)

        /**
         * 写入原有条件分支表达式。
         *
         * @param builder 变量批处理函数正文。
         */
        override fun appendDisplayEvaluation(builder: StringBuilder) {
            builder.append("val ")
            builder.append(id)
            builder.append(" = (")
            builder.append(expression)
            builder.append(")\n")
        }

        /**
         * 从末分支生成条件，保留未匹配返回 false 的既有条件变量契约。
         *
         * @param values 按配置顺序排列的分支。
         * @return Nova 条件表达式。
         * @throws IllegalArgumentException 分支为空或缺少表达式时抛出。
         */
        private fun createExpression(values: List<ConfigurationSection>): String {
            require(values.isNotEmpty()) { "变量 '$id' 必须至少声明一个条件分支。" }
            var generated = "false"
            for (index in values.size - 1 downTo 0) {
                val section = values[index]
                val condition = section.getString("condition")
                require(condition != null && condition.isNotBlank()) { "变量 '$id' 的分支 $index 缺少 condition。" }
                val action = section.getString("action")
                require(action != null && action.isNotBlank()) { "变量 '$id' 的分支 $index 缺少 action。" }
                generated = "if ($condition) ($action) else ($generated)"
            }
            return generated
        }
    }
}
