package com.gitee.planners.core.config

import com.gitee.planners.api.job.Variable
import com.gitee.planners.module.script.ScriptOptions
import com.gitee.planners.module.script.ScriptManager
import com.gitee.planners.module.script.SingletonScript
import com.gitee.scriptengine.api.ScriptSession
import taboolib.common5.cbool
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
    fun evaluate(session: ScriptSession): Any?

    companion object {

        private val identifierPattern = Regex("^[A-Za-z_][A-Za-z0-9_]*$")

        fun parse(id: String, value: Any): ImmutableVariable {
            require(identifierPattern.matches(id)) {
                "变量 ID '$id' 必须是合法 JavaScript 标识符，只允许字母、数字和下划线，且不能以数字开头"
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

    open class Default(override val id: String, action: String) : SingletonScript(action), ImmutableVariable {

        /**
         * 计算当前变量，并将结果同步写入当前 JavaScript 全局作用域。
         *
         * @param session 当前脚本会话。
         * @return 变量计算结果。
         */
        override fun evaluate(session: ScriptSession): Any? {
            return evaluateFor(session, id)
        }

        /**
         * 计算变量表达式并写入指定的 JavaScript 变量名。
         *
         * @param session 当前脚本会话。
         * @param targetId 接收计算结果的变量 ID。
         * @return 变量计算结果。
         */
        internal fun evaluateFor(session: ScriptSession, targetId: String): Any? {
            val action = action
            if (action.isEmpty()) {
                return null
            }
            val value = invoke(session)
            ScriptManager.setReusableSessionBinding(session, targetId, value)
            return value
        }
    }

    class Case(condition: String, id: String, action: String) : Default(id, action) {

        val condition = SingletonScript(condition)

        /**
         * 玩家是否匹配条件
         */
        fun match(options: ScriptOptions): Boolean {
            return condition.run(options).thenApply { it.cbool }.getNow(false)
        }

        /**
         * 在当前会话内计算分支条件。
         *
         * @param session 当前脚本会话。
         * @return 条件成立时为 true。
         */
        fun matches(session: ScriptSession): Boolean {
            val value = condition.invoke(session)
            return value.cbool
        }

    }

    class When(override val id: String, values: List<ConfigurationSection>) : ImmutableVariable {

        private val cases = values.map {
            val id = it.getString("id", "__CASE__")!!
            val condition = it.getString("condition", "true")!!
            val action = it.getString("action", "null")!!
            Case(condition, id, action)
        }

        override fun run(options: ScriptOptions): CompletableFuture<Any?> {
            val case = cases.firstOrNull { it.match(options) } ?: return CompletableFuture.completedFuture(false)

            return case.run(options)
        }

        /**
         * 在同一个会话中按配置顺序匹配并计算条件变量。
         *
         * @param session 当前脚本会话。
         * @return 首个匹配分支的结果；没有匹配分支时为 false。
         */
        override fun evaluate(session: ScriptSession): Any? {
            for (case in cases) {
                if (case.matches(session)) {
                    return case.evaluateFor(session, id)
                }
            }
            return false
        }

    }

}
