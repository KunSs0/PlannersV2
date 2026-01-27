package com.gitee.planners.module.fluxon.economy

import com.gitee.planners.module.fluxon.FluxonScriptCache
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionContext
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 经济系统扩展 (Vault Economy)
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

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // getBalance() -> double (从环境获取player)
        runtime.registerFunction("getBalance", FunctionSignature.returns(Type.D).noParams()) { ctx ->
            val player = getPlayerFromEnv(ctx)
            val eco = economy ?: return@registerFunction
            ctx.setReturnDouble(eco.getBalance(player))
        }

        // getBalance(player) -> double
        runtime.registerFunction("getBalance", FunctionSignature.returns(Type.D).params(Type.OBJECT)) { ctx ->
            val player = ctx.getRef(0) as? Player ?: return@registerFunction
            val eco = economy ?: return@registerFunction
            ctx.setReturnDouble(eco.getBalance(player))
        }

        // takeBalance(amount) -> boolean (从环境获取player)
        runtime.registerFunction("takeBalance", FunctionSignature.returns(Type.Z).params(Type.D)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val player = getPlayerFromEnv(ctx)
            val eco = economy ?: return@registerFunction
            ctx.setReturnBool(eco.withdrawPlayer(player, amount).transactionSuccess())
        }

        // takeBalance(amount, player) -> boolean
        runtime.registerFunction("takeBalance", FunctionSignature.returns(Type.Z).params(Type.D, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val player = ctx.getRef(1) as? Player ?: return@registerFunction
            val eco = economy ?: return@registerFunction
            ctx.setReturnBool(eco.withdrawPlayer(player, amount).transactionSuccess())
        }

        // giveBalance(amount) -> boolean (从环境获取player)
        runtime.registerFunction("giveBalance", FunctionSignature.returns(Type.Z).params(Type.D)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val player = getPlayerFromEnv(ctx)
            val eco = economy ?: return@registerFunction
            ctx.setReturnBool(eco.depositPlayer(player, amount).transactionSuccess())
        }

        // giveBalance(amount, player) -> boolean
        runtime.registerFunction("giveBalance", FunctionSignature.returns(Type.Z).params(Type.D, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val player = ctx.getRef(1) as? Player ?: return@registerFunction
            val eco = economy ?: return@registerFunction
            ctx.setReturnBool(eco.depositPlayer(player, amount).transactionSuccess())
        }

        // setBalance(amount) -> void (从环境获取player)
        runtime.registerFunction("setBalance", FunctionSignature.returns(Type.VOID).params(Type.D)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val player = getPlayerFromEnv(ctx)
            val eco = economy ?: return@registerFunction
            val current = eco.getBalance(player)
            if (current > amount) {
                eco.withdrawPlayer(player, current - amount)
            } else {
                eco.depositPlayer(player, amount - current)
            }
        }

        // setBalance(amount, player) -> void
        runtime.registerFunction("setBalance", FunctionSignature.returns(Type.VOID).params(Type.D, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val player = ctx.getRef(1) as? Player ?: return@registerFunction
            val eco = economy ?: return@registerFunction
            val current = eco.getBalance(player)
            if (current > amount) {
                eco.withdrawPlayer(player, current - amount)
            } else {
                eco.depositPlayer(player, amount - current)
            }
        }
    }

    private fun getPlayerFromEnv(ctx: FunctionContext<*>): Player {
        val find = ctx.environment.rootVariables["player"]
        if (find is Player) {
            return find
        }
        throw IllegalStateException("No player found in environment")
    }
}
