package com.gitee.planners.module.fluxon.common

import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.getSenderArg
import com.gitee.planners.module.fluxon.registerFunction
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 通用扩展函数
 */
object CommonExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // tell(message, [sender]) -> void
        runtime.registerFunction("tell", listOf(1, 2)) { ctx ->
            val message = ctx.arguments[0]?.toString() ?: return@registerFunction null
            val sender = ctx.getSenderArg(1)
            sender.sendMessage(message)
            null
        }
    }
}
