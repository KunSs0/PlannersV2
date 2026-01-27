package com.gitee.planners.module.fluxon.economy

import com.gitee.planners.module.fluxon.FluxonFunctionContext
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.registerFunction
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 经济系统扩展 (Vault Economy)
 */
object EconomyExtensions {

    private val economy: Economy? by lazy {
        try {
            Bukkit.getServer().servicesManager.getRegistration(Economy::class.java)?.provider
        } catch (e: Exception) {
            null
        }
    }

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // getBalance([player]) -> double
        runtime.registerFunction("getBalance", listOf(0, 1)) { ctx ->
            val player = ctx.getPlayerArg(0)
            economy?.getBalance(player) ?: 0.0
        }

        // takeBalance(amount, [player]) -> boolean
        runtime.registerFunction("takeBalance", listOf(1, 2)) { ctx ->
            val amount = (ctx.arguments[0] as Number).toDouble()
            val player = ctx.getPlayerArg(1)
            economy?.withdrawPlayer(player, amount)?.transactionSuccess() ?: false
        }

        // giveBalance(amount, [player]) -> boolean
        runtime.registerFunction("giveBalance", listOf(1, 2)) { ctx ->
            val amount = (ctx.arguments[0] as Number).toDouble()
            val player = ctx.getPlayerArg(1)
            economy?.depositPlayer(player, amount)?.transactionSuccess() ?: false
        }

        // setBalance(amount, [player]) -> void
        runtime.registerFunction("setBalance", listOf(1, 2)) { ctx ->
            val amount = (ctx.arguments[0] as Number).toDouble()
            val player = ctx.getPlayerArg(1)
            val eco = economy ?: return@registerFunction null
            val current = eco.getBalance(player)
            if (current > amount) {
                eco.withdrawPlayer(player, current - amount)
            } else {
                eco.depositPlayer(player, amount - current)
            }
            null
        }
    }

    private fun FluxonFunctionContext.getPlayerArg(index: Int): Player {
        if (arguments.size > index) {
            return arguments[index] as? Player
                ?: throw IllegalStateException("Argument at $index is not a player")
        }
        return environment.rootVariables["player"] as? Player
            ?: throw IllegalStateException("No player found in environment")
    }
}
