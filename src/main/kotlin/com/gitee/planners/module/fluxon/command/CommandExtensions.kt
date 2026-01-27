package com.gitee.planners.module.fluxon.command

import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type

/**
 * Command 命令执行扩展
 */
object CommandExtensions {

    fun register() {
        val runtime = FluxonScriptCache.runtime

        // Player 命令执行扩展
        runtime.registerExtension(Player::class.java)
            .function("executeCommand", FunctionSignature.returns(Type.VOID).params(Type.OBJECT)) { ctx ->
                val player = ctx.target ?: return@function
                val command = ctx.getRef(0)?.toString() ?: return@function
                player.performCommand(command)
            }
            .function("executeCommandAsOp", FunctionSignature.returns(Type.VOID).params(Type.OBJECT)) { ctx ->
                val player = ctx.target ?: return@function
                val command = ctx.getRef(0)?.toString() ?: return@function
                val wasOp = player.isOp
                try {
                    player.isOp = true
                    player.performCommand(command)
                } finally {
                    player.isOp = wasOp
                }
            }

        // 注册全局命令函数（作为静态扩展）
        // 通过 String 类的扩展来实现全局命令执行
        runtime.registerExtension(String::class.java)
            .function("executeAsConsole", FunctionSignature.returns(Type.VOID).noParams()) { ctx ->
                val command = ctx.target ?: return@function
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            }
    }

    /**
     * 注册全局命令函数
     * 可以通过 FluxonScript 直接调用
     */
    fun registerGlobalFunctions() {
        // 注册全局函数：executeConsoleCommand
        // TODO: Fluxon 需要支持全局函数注册
        // val runtime = FluxonScriptCache.runtime
        // runtime.registerGlobalFunction("executeConsoleCommand", ...) { command ->
        //     Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.toString())
        // }
    }
}
