package com.gitee.planners.api.common.script

import taboolib.module.kether.KetherShell
import taboolib.module.kether.ScriptOptions
import java.util.concurrent.CompletableFuture

open class SingletonKetherScript(string: String? = null) : KetherScript {

    open val action = string ?: ""

    val isNotNull: Boolean
        get() = action.isNotEmpty()

    override fun run(options: KetherScriptOptions): CompletableFuture<Any?> {
        return KetherShell.eval(action, options.build().build())
    }


}
