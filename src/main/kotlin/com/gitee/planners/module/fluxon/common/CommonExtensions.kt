package com.gitee.planners.module.fluxon.common

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.getTargetsArg
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 通用扩展函数
 */
object CommonExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // tell(message) - 发送消息给 sender
        runtime.registerFunction("tell", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val message = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.CommandSender<*>>().forEach { target ->
                target.sendMessage(message)
            }
        }

        // tell(message, targets) - 发送消息给目标
        runtime.registerFunction("tell", returns(Type.VOID).params(Type.STRING, Type.OBJECT)) { ctx ->
            val message = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.CommandSender<*>>().forEach { target ->
                target.sendMessage(message)
            }
        }
    }
}
