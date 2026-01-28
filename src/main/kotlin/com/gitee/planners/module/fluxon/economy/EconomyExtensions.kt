package com.gitee.planners.module.fluxon.economy

import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.getPlayerArg
import com.gitee.planners.module.fluxon.registerFunction
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.Requires

/**
 * 经济系统扩展 (Vault Economy)
 */
@Requires(classes = ["net.milkbowl.vault.economy.Economy"])
object EconomyExtensions {

    private val economy: Economy? by lazy {
        try {
            Bukkit.getServer().servicesManager.getRegistration(Economy::class.java)?.provider
        } catch (e: Exception) {
            null
        }
    }

    @Awake(LifeCycle.LOAD)
    fun init() {
        val runtime = FluxonScriptCache.runtime

        runtime.registerFunction("getBalance", listOf(0, 1)) { ctx ->
            val player = ctx.getPlayerArg(0) ?: return@registerFunction 0.0
            economy?.getBalance(player) ?: 0.0
        }

        runtime.registerFunction("takeMoney", listOf(1, 2)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val player = ctx.getPlayerArg(1) ?: return@registerFunction false
            economy?.withdrawPlayer(player, amount)?.transactionSuccess() ?: false
        }

        runtime.registerFunction("giveMoney", listOf(1, 2)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val player = ctx.getPlayerArg(1) ?: return@registerFunction false
            economy?.depositPlayer(player, amount)?.transactionSuccess() ?: false
        }

        runtime.registerFunction("setMoney", listOf(1, 2)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val player = ctx.getPlayerArg(1) ?: return@registerFunction null
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
}
