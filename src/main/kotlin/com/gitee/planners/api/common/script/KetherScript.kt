package com.gitee.planners.api.common.script

import java.util.concurrent.CompletableFuture
import java.util.function.Function

interface KetherScript : Script {

    fun run(options: KetherScriptOptions): CompletableFuture<Any?>

    fun <T> get(options: KetherScriptOptions, parser: Function<Any?, T>): T {
        return parser.apply(run(options).get())
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
