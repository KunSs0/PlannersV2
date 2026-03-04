package com.gitee.planners.api.job

import com.gitee.planners.module.script.ScriptOptions
import com.gitee.planners.module.script.SingletonScript
import taboolib.common5.cbool
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration
import java.util.concurrent.CompletableFuture

interface Condition {

    fun match(options: ScriptOptions): CompletableFuture<Boolean>

    fun consumeTo(options: ScriptOptions)

    class Messaged(val condition: String, message: String?, post: String?) : Condition,
        SingletonScript(condition) {

        val post = SingletonScript(post)

        private val message = SingletonScript(message)

        override fun match(options: ScriptOptions): CompletableFuture<Boolean> {
            return this.run(options).thenApply { it.cbool }
        }

        fun getMessage(options: ScriptOptions): String {
            if (message.isNotNull) {
                return message.eval(options)?.toString() ?: ""
            }
            return ""
        }

        override fun consumeTo(options: ScriptOptions) {
            if (post.isNotNull) { post.run(options) }
        }

    }

    class Combined(private val values: List<Condition>) : Condition {

        override fun consumeTo(options: ScriptOptions) {
            values.forEach { it.consumeTo(options) }
        }

        fun getMessage(options: ScriptOptions): List<String> {
            return values.filterIsInstance<Messaged>().map { it.getMessage(options) }.filter { it.isNotEmpty() }
        }

        override fun match(options: ScriptOptions): CompletableFuture<Boolean> {
            return CompletableFuture.completedFuture(this.values.all {
                it.match(options).getNow(false)
            })
        }

        /**
         * the sync method
         */
        fun verify(options: ScriptOptions): VerifyInfo {
            return VerifyInfo(this.values.filter {
                !it.match(options).getNow(false)
            })
        }

    }

    class VerifyInfo(val unsuccessful: List<Condition>) {

        val isValid = unsuccessful.isEmpty()

        val isInvalid = !isValid

    }

    companion object {

        fun combined(conf: Any?): Combined {
            return Combined(when {

                conf == null -> emptyList()

                conf is List<*> -> conf.map { parse(Configuration.fromMap(it as Map<*, *>)) }

                conf is ConfigurationSection && conf.contains("if") -> listOf(parse(conf))

                conf is ConfigurationSection -> conf.getKeys(false).map { parse(conf.getConfigurationSection(it)!!) }

                else -> throw IllegalArgumentException("Cannot combine multiple values with $conf")
            })
        }

        fun parse(conf: ConfigurationSection): Condition {
            val condition = conf.getString("if", "true")!!
            val message = conf.getString("message", "false")
            return Messaged(condition, message, conf.getString("post"))
        }

    }

}
