package com.gitee.planners.core.config

import com.gitee.planners.api.job.Variable
import com.gitee.planners.module.script.ScriptOptions
import com.gitee.planners.module.script.SingletonScript
import taboolib.common5.cbool
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration
import java.util.concurrent.CompletableFuture

interface ImmutableVariable : Variable {

    companion object {

        fun parse(id: String, value: Any): Variable {
            return when (value) {

                is String -> Default(id, value)

                is Boolean, is Int, is Float, is Double, is Long -> Default(id, "$value")

                is List<*> -> {
                    val first = value.firstOrNull()
                    if (first is Map<*, *>) {
                        When(id, value.map { Configuration.fromMap(it as Map<*, *>) })
                    } else {
                        DirectList(id, value)
                    }
                }

                else -> error("Unsupported value type ${value::class.java}")
            }
        }

    }

    open class Default(override val id: String, action: String) : SingletonScript(action), ImmutableVariable

    /** 直接注入列表值，不经过脚本求值（用于 YAML 数组变量如 rectX: [4, 1, 3, 4]） */
    class DirectList(override val id: String, private val list: List<*>) : ImmutableVariable {
        override fun run(options: ScriptOptions): CompletableFuture<Any?> {
            return CompletableFuture.completedFuture(list)
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

    }

}
