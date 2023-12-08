package com.gitee.planners.api.script

import taboolib.module.kether.KetherShell
import taboolib.module.kether.ScriptOptions
import java.util.concurrent.CompletableFuture

open class SingletonKetherScript(string: String? = null) : KetherScript {

    open val action = string ?: "null"

    val isNotNull: Boolean
        get() = action == "null"

    override fun run(options: KetherScriptOptions): CompletableFuture<Any?> {
        return KetherShell.eval(action, options.build().build())
    }


}
