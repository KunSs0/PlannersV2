package com.gitee.planners.module.fluxon.economy

import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.getPlayerArg
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
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

        // getBalance() - 获取 sender 余额
        runtime.registerFunction("getBalance", returns(Type.NUMBER).noParams()) { ctx ->
            val player = ctx.getPlayerArg(-1)
            ctx.setReturnDouble(if (player != null) economy?.getBalance(player) ?: 0.0 else 0.0)
        }

        // getBalance(player) - 获取指定玩家余额
        runtime.registerFunction("getBalance", returns(Type.NUMBER).params(Type.OBJECT)) { ctx ->
            val player = ctx.getPlayerArg(0)
            ctx.setReturnDouble(if (player != null) economy?.getBalance(player) ?: 0.0 else 0.0)
        }

        // takeMoney(amount) - 从 sender 扣款
        runtime.registerFunction("takeMoney", returns(Type.BOOLEAN).params(Type.NUMBER)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val player = ctx.getPlayerArg(-1)
            ctx.setReturnBool(player != null && economy?.withdrawPlayer(player, amount)?.transactionSuccess() == true)
        }

        // takeMoney(amount, player) - 从指定玩家扣款
        runtime.registerFunction("takeMoney", returns(Type.BOOLEAN).params(Type.NUMBER, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val player = ctx.getPlayerArg(1)
            ctx.setReturnBool(player != null && economy?.withdrawPlayer(player, amount)?.transactionSuccess() == true)
        }

        // giveMoney(amount) - 给 sender 加款
        runtime.registerFunction("giveMoney", returns(Type.BOOLEAN).params(Type.NUMBER)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val player = ctx.getPlayerArg(-1)
            ctx.setReturnBool(player != null && economy?.depositPlayer(player, amount)?.transactionSuccess() == true)
        }

        // giveMoney(amount, player) - 给指定玩家加款
        runtime.registerFunction("giveMoney", returns(Type.BOOLEAN).params(Type.NUMBER, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val player = ctx.getPlayerArg(1)
            ctx.setReturnBool(player != null && economy?.depositPlayer(player, amount)?.transactionSuccess() == true)
        }

        // setMoney(amount) - 设置 sender 余额
        runtime.registerFunction("setMoney", returns(Type.VOID).params(Type.NUMBER)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val player = ctx.getPlayerArg(-1) ?: return@registerFunction
            setBalance(player, amount)
        }

        // setMoney(amount, player) - 设置指定玩家余额
        runtime.registerFunction("setMoney", returns(Type.VOID).params(Type.NUMBER, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val player = ctx.getPlayerArg(1) ?: return@registerFunction
            setBalance(player, amount)
        }
    }

    private fun setBalance(player: org.bukkit.entity.Player, amount: Double) {
        val eco = economy ?: return
        val current = eco.getBalance(player)
        if (current > amount) {
            eco.withdrawPlayer(player, current - amount)
        } else {
            eco.depositPlayer(player, amount - current)
        }
    }
}
