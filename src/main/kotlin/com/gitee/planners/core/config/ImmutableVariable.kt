package com.gitee.planners.core.config

import com.gitee.planners.api.job.Variable
import com.gitee.planners.module.script.ScriptOptions
import com.gitee.planners.module.script.ScriptManager
import com.gitee.planners.module.script.SingletonScript
import com.gitee.planners.module.script.NovaSession
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration
import java.util.concurrent.CompletableFuture

interface ImmutableVariable : Variable {

    /**
     * 在已打开的脚本会话中计算变量值。
     *
     * @param session 当前图标渲染使用的脚本会话。
     * @return 变量计算结果。
     */
    fun evaluate(session: NovaSession): Any?

    /** 向技能显示变量批处理函数写入局部计算语句。 */
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

    open class Default(override val id: String, action: String) : SingletonScript(action, "variable:$id:action"), ImmutableVariable {

        /**
         * 计算当前变量，并将结果同步写入当前 Nova 会话绑定。
         *
         * @param session 当前脚本会话。
         * @return 变量计算结果。
         */
        override fun evaluate(session: NovaSession): Any? {
            return evaluateFor(session, id)
        }

        override fun appendDisplayEvaluation(builder: StringBuilder) {
            builder.append("val ")
            builder.append(id)
            builder.append(" = (")
            builder.append(action)
            builder.append(")\n")
        }

        /**
         * 计算变量表达式并写入指定的 Nova 变量名。
         *
         * @param session 当前脚本会话。
         * @param targetId 接收计算结果的变量 ID。
         * @return 变量计算结果。
         */
        internal fun evaluateFor(session: NovaSession, targetId: String): Any? {
            val action = action
            if (action.isEmpty()) {
                return null
            }
            val value = invoke(session)
            ScriptManager.setReusableSessionBinding(session, targetId, value)
            return value
        }
    }

    /**
     * 由 YAML 条件分支生成单个 Nova 表达式的变量。
     *
     * Kotlin 仅负责把声明顺序映射为 Nova `if ... else` 源码；分支判断和结果计算均由
     * RuntimeWorkspace 预编译后的 Nova 入口执行。
     */
    class When(override val id: String, values: List<ConfigurationSection>) : ImmutableVariable {

        private val expression = createExpression(values)
        private val script = SingletonScript(expression, "variable:$id:branches")

        /** 使用预编译 Nova 分支表达式计算变量。 */
        override fun run(options: ScriptOptions): CompletableFuture<Any?> {
            return script.run(options)
        }

        /**
         * 在同一个会话中按配置顺序匹配并计算条件变量。
         *
         * @param session 当前脚本会话。
         * @return 首个匹配分支的结果；没有匹配分支时为 false。
         */
        override fun evaluate(session: NovaSession): Any? {
            val value = script.invoke(session)
            ScriptManager.setReusableSessionBinding(session, id, value)
            return value
        }

        /** 将同一 Nova 分支表达式写入显示变量批处理函数。 */
        override fun appendDisplayEvaluation(builder: StringBuilder) {
            builder.append("val ")
            builder.append(id)
            builder.append(" = (")
            builder.append(expression)
            builder.append(")\n")
        }

        /** 按声明逆序折叠成保持首个匹配语义的 Nova `if ... else` 表达式。 */
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
