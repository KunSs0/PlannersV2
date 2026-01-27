package com.gitee.planners.module.fluxon.sender

import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.command.CommandSender
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type

/**
 * CommandSender 扩展函数
 */
object SenderExtensions {

    fun register() {
        val runtime = FluxonScriptCache.runtime

        // CommandSender 扩展 - 发送消息
        runtime.registerExtension(CommandSender::class.java)
            .function("sendMessage", FunctionSignature.returns(Type.VOID).params(Type.OBJECT)) { ctx ->
                val sender = ctx.target ?: return@function
                val message = ctx.getRef(0)?.toString() ?: return@function
                sender.sendMessage(message)
            }
            .function("hasPermission", FunctionSignature.returns(Type.Z).params(Type.OBJECT)) { ctx ->
                val sender = ctx.target ?: return@function
                val permission = ctx.getRef(0)?.toString() ?: return@function
                ctx.setReturnBool(sender.hasPermission(permission))
            }
            .function("name", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
                val sender = ctx.target ?: return@function
                ctx.setReturnRef(sender.name)
            }

        // ProxyTarget.BukkitEntity (实现了 CommandSender) 扩展
        runtime.registerExtension(ProxyTarget.BukkitEntity::class.java)
            .function("sendMessage", FunctionSignature.returns(Type.VOID).params(Type.OBJECT)) { ctx ->
                val target = ctx.target ?: return@function
                val message = ctx.getRef(0)?.toString() ?: return@function
                target.sendMessage(message)
            }
            .function("hasPermission", FunctionSignature.returns(Type.Z).params(Type.OBJECT)) { ctx ->
                val target = ctx.target ?: return@function
                val permission = ctx.getRef(0)?.toString() ?: return@function
                ctx.setReturnBool(target.instance.hasPermission(permission))
            }

        // ProxyTarget.Console 扩展
        runtime.registerExtension(ProxyTarget.Console::class.java)
            .function("sendMessage", FunctionSignature.returns(Type.VOID).params(Type.OBJECT)) { ctx ->
                val target = ctx.target ?: return@function
                val message = ctx.getRef(0)?.toString() ?: return@function
                target.sendMessage(message)
            }
            .function("hasPermission", FunctionSignature.returns(Type.Z).params(Type.OBJECT)) { ctx ->
                val target = ctx.target ?: return@function
                val permission = ctx.getRef(0)?.toString() ?: return@function
                ctx.setReturnBool(target.instance.hasPermission(permission))
            }
    }
}
