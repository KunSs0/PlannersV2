package com.gitee.planners.module.fluxon.common

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.getTargetsArg
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

        // tell(message, [targets]) - 发送消息
        runtime.registerFunction("tell", listOf(1, 2)) { ctx ->
            val message = ctx.arguments[0]?.toString() ?: return@registerFunction null
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.CommandSender<*>>().forEach { target ->
                target.sendMessage(message)
            }
            null
        }
    }
}
