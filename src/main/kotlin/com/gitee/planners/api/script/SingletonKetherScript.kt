package com.gitee.planners.api.script

import taboolib.module.kether.KetherShell
import taboolib.module.kether.ScriptOptions
import java.util.concurrent.CompletableFuture

open class SingletonKetherScript(string: String? = null) : KetherScript {

    open val action = string ?: "null"

    override fun run(block: ScriptOptions.ScriptOptionsBuilder.() -> Unit): CompletableFuture<Any?> {
        return KetherShell.eval(action, options = ScriptOptions.new { block(this) })
    }


}
