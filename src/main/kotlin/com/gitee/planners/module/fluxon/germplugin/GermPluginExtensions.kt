package com.gitee.planners.module.fluxon.germplugin

import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type

/**
 * GermPlugin 集成扩展
 * 通过 Player 扩展与 GermPlugin 交互
 */
object GermPluginExtensions {

    fun register() {
        val runtime = FluxonScriptCache.runtime

        // Player GermPlugin 扩展
        runtime.registerExtension(Player::class.java)
            .function("playGermModel", FunctionSignature.returns(Type.VOID).params(Type.OBJECT)) { ctx ->
                val player = ctx.target ?: return@function
                val model = ctx.getRef(0)?.toString() ?: return@function

                val command = "gp model cast ${player.name} $model"
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            }
            .function("stopGermModel", FunctionSignature.returns(Type.VOID).params(Type.OBJECT)) { ctx ->
                val player = ctx.target ?: return@function
                val model = ctx.getRef(0)?.toString() ?: return@function

                val command = "gp model stop ${player.name} $model"
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            }
            .function("playGermEffect", FunctionSignature.returns(Type.VOID).params(Type.OBJECT)) { ctx ->
                val player = ctx.target ?: return@function
                val effect = ctx.getRef(0)?.toString() ?: return@function

                val command = "gp effect spawn ${player.name} $effect"
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            }
            .function("stopGermEffect", FunctionSignature.returns(Type.VOID).params(Type.OBJECT)) { ctx ->
                val player = ctx.target ?: return@function
                val effect = ctx.getRef(0)?.toString() ?: return@function

                val command = "gp effect stop ${player.name} $effect"
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            }
            .function("playGermSound", FunctionSignature.returns(Type.VOID).params(Type.OBJECT)) { ctx ->
                val player = ctx.target ?: return@function
                val sound = ctx.getRef(0)?.toString() ?: return@function

                val command = "gp sound play ${player.name} $sound player"
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            }
    }
}
