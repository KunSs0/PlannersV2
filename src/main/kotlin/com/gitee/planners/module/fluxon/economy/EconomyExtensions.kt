package com.gitee.planners.module.fluxon.economy

import com.gitee.planners.module.fluxon.FluxonScriptCache
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type

/**
 * 经济系统扩展 (Vault Economy)
 * 提供 balance 命令支持
 */
object EconomyExtensions {

    private val economy: Economy? by lazy {
        try {
            val rsp = Bukkit.getServer().servicesManager.getRegistration(Economy::class.java)
            rsp?.provider
        } catch (e: Exception) {
            null
        }
    }

    fun register() {
        val runtime = FluxonScriptCache.runtime

        // Player 经济扩展
        runtime.registerExtension(Player::class.java)
            // balance get - 获取余额
            .function("getBalance", FunctionSignature.returns(Type.D).noParams()) { ctx ->
                val player = ctx.target ?: return@function
                val eco = economy ?: return@function
                ctx.setReturnDouble(eco.getBalance(player))
            }
            // balance take - 扣除金额
            .function("takeBalance", FunctionSignature.returns(Type.Z).params(Type.D)) { ctx ->
                val player = ctx.target ?: return@function
                val amount = ctx.getAsDouble(0)
                val eco = economy ?: return@function
                val success = eco.withdrawPlayer(player, amount).transactionSuccess()
                ctx.setReturnBool(success)
            }
            // balance give - 增加金额
            .function("giveBalance", FunctionSignature.returns(Type.Z).params(Type.D)) { ctx ->
                val player = ctx.target ?: return@function
                val amount = ctx.getAsDouble(0)
                val eco = economy ?: return@function
                val success = eco.depositPlayer(player, amount).transactionSuccess()
                ctx.setReturnBool(success)
            }
            // balance set - 设置金额
            .function("setBalance", FunctionSignature.returns(Type.VOID).params(Type.D)) { ctx ->
                val player = ctx.target ?: return@function
                val amount = ctx.getAsDouble(0)
                val eco = economy ?: return@function
                val current = eco.getBalance(player)
                if (current > amount) {
                    eco.withdrawPlayer(player, current - amount)
                } else {
                    eco.depositPlayer(player, amount - current)
                }
            }
    }
}
