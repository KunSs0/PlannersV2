package com.gitee.planners.api.common.script

import java.util.concurrent.CompletableFuture

interface KetherScript : Script {

    fun run(options: KetherScriptOptions): CompletableFuture<Any?>

    fun <T> get(options: KetherScriptOptions,parser: (Any?) -> T): T {
        return parser.invoke(run(options).get())
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
