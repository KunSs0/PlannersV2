package com.gitee.planners.api.common.script

import taboolib.common5.cdouble
import taboolib.common5.cint
import java.util.concurrent.CompletableFuture
import java.util.function.Function

interface KetherScript : Script {

    fun run(options: KetherScriptOptions): CompletableFuture<Any?>

    companion object {

        val PARSER_INT = Function<Any?, Int> { it.cint }

        val PARSER_DOUBLE = Function<Any?, Double> { it.cdouble }

        val PARSER_STRING = Function<Any?, String> { it.toString() }

        val PARSER_BOOLEAN = Function<Any?, Boolean> { it.toString().toBoolean() }

        val PARSER_ANY = Function<Any?, Any?> { it }

        val PARSER_VOID = Function<Any?, Unit> { }


        inline fun <reified T> KetherScript.getNow(options: KetherScriptOptions, parser: Function<Any?, T>): T {
            return parser.apply(run(options).getNow(null))
        }

    }

    enum class RuntimeEnvironment {
        SKILL {
            override fun getScriptPlatform(): ComplexScriptPlatform {
                return ComplexScriptPlatform.SKILL
            }
        };

        abstract fun getScriptPlatform(): ComplexScriptPlatform

    }

}
