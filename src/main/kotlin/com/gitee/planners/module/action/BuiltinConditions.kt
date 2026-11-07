package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryEvaluator
import com.gitee.planners.module.action.query.QueryRef
import com.gitee.planners.module.action.query.QueryScope
import java.util.concurrent.ThreadLocalRandom

/**
 * 内置条件实现。
 *
 * 只包含与宿主无关的通用条件；技能上下文的判定分别在 `planners.*` 与
 * `fc.*` 文件中维护。
 */
object BuiltinConditions {

    /**
     * 任意求值表达式。
     *
     * 参数 `e` 是整段求值表达式，路径裸写不包 `<>`。
     */
    object Expression : ActionCondition {

        override val id = "expr"

        override val aliases = listOf("expr", "expression")

        override fun validate(arguments: ActionArguments) {
            arguments.rejectUnknown("e", "expr", "expression")
            val source = arguments.raw("e", "expr", "expression")
            if (source == null) {
                throw IllegalArgumentException("${arguments.path} requires argument 'e'.")
            }
            QueryEvaluator.validate(source, "${arguments.path}.e")
        }

        override fun compile(arguments: ActionArguments): Any? {
            val source = arguments.raw("e", "expr", "expression")
            if (source == null) {
                throw IllegalArgumentException("${arguments.path} requires argument 'e'.")
            }
            return QueryRef.parseExpression(source, "${arguments.path}.e")
        }

        override fun test(scope: QueryScope, arguments: ActionArguments, compiled: Any?): Boolean {
            if (compiled !is QueryRef) {
                throw IllegalStateException("${arguments.path} expression was not compiled.")
            }
            return compiled.resolveBoolean(scope)
        }
    }

    /**
     * 概率条件。
     */
    object Chance : ActionCondition {

        override val id = "chance"

        override val aliases = listOf("chance")

        override fun validate(arguments: ActionArguments) {
            arguments.rejectUnknown("value", "v")
            if (!arguments.contains("value") && !arguments.contains("v")) {
                throw IllegalArgumentException("${arguments.path} requires argument 'value'.")
            }
        }

        override fun test(scope: QueryScope, arguments: ActionArguments, compiled: Any?): Boolean {
            val value = arguments.requireNumber(scope, "value", "v")
            if (value < 0.0 || value > 1.0) {
                throw IllegalArgumentException("${arguments.path} value must be between 0 and 1.")
            }
            return ThreadLocalRandom.current().nextDouble() < value
        }
    }
}
