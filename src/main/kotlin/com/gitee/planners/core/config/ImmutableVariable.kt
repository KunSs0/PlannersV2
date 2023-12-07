package com.gitee.planners.core.config

import com.gitee.planners.api.script.SingletonKetherScript
import com.gitee.planners.api.job.Variable
import com.gitee.planners.api.script.KetherScriptOptions
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration
import taboolib.module.kether.ScriptOptions
import taboolib.module.kether.runKether
import java.util.concurrent.CompletableFuture

interface ImmutableVariable : Variable {

    companion object {

        fun parse(id: String, value: Any): Variable {
            return when (value) {

                is String -> Default(id, value)

                is Boolean -> Default(id, "$value")

                is List<*> -> {
                    When(id, value.map { Configuration.fromMap(it as Map<*, *>) })
                }

                else -> error("Unsupported value type ${value::class.java}")
            }
        }

    }

    open class Default(override val id: String, action: String) : SingletonKetherScript(action), ImmutableVariable

    class Case(condition: String, id: String, action: String) : Default(id, action) {

        val condition = SingletonKetherScript(condition)

        /**
         * 玩家是否匹配条件
         */
        fun match(options: KetherScriptOptions): Boolean {
            return runKether(false) { condition.run(options) }!! as Boolean
        }

    }

    class When(override val id: String, values: List<ConfigurationSection>) : ImmutableVariable {

        private val cases = values.map {
            val id = it.getString("id", "__CASE__")!!
            val condition = it.getString("condition", "true")!!
            val action = it.getString("action", "null")!!
            Case(condition, id, action)
        }

        override fun run(options: KetherScriptOptions): CompletableFuture<Any?> {
            val case = cases.firstOrNull { it.match(options) } ?: return CompletableFuture.completedFuture(false)

            return case.run(options)
        }

    }

}
