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

        /**
         * 获取 sender 的账户余额
         * @return 余额数值，无经济插件时返回 0
         */
        runtime.registerFunction("getBalance", returns(Type.NUMBER).noParams()) { ctx ->
            val player = ctx.getPlayerArg(-1)
            ctx.setReturnDouble(if (player != null) economy?.getBalance(player) ?: 0.0 else 0.0)
        }

        /**
         * 获取指定玩家的账户余额
         * @param player 目标玩家
         * @return 余额数值
         */
        runtime.registerFunction("getBalance", returns(Type.NUMBER).params(Type.OBJECT)) { ctx ->
            val player = ctx.getPlayerArg(0)
            ctx.setReturnDouble(if (player != null) economy?.getBalance(player) ?: 0.0 else 0.0)
        }

        /**
         * 从 sender 账户扣款
         * @param amount 扣款金额
         * @return 是否扣款成功（余额不足或无经济插件返回 false）
         */
        runtime.registerFunction("takeMoney", returns(Type.BOOLEAN).params(Type.NUMBER)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val player = ctx.getPlayerArg(-1)
            ctx.setReturnBool(player != null && economy?.withdrawPlayer(player, amount)?.transactionSuccess() == true)
        }

        /**
         * 从指定玩家账户扣款
         * @param amount 扣款金额
         * @param player 目标玩家
         * @return 是否扣款成功
         */
        runtime.registerFunction("takeMoney", returns(Type.BOOLEAN).params(Type.NUMBER, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val player = ctx.getPlayerArg(1)
            ctx.setReturnBool(player != null && economy?.withdrawPlayer(player, amount)?.transactionSuccess() == true)
        }

        /**
         * 向 sender 账户存款
         * @param amount 存款金额
         * @return 是否存款成功
         */
        runtime.registerFunction("giveMoney", returns(Type.BOOLEAN).params(Type.NUMBER)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val player = ctx.getPlayerArg(-1)
            ctx.setReturnBool(player != null && economy?.depositPlayer(player, amount)?.transactionSuccess() == true)
        }

        /**
         * 向指定玩家账户存款
         * @param amount 存款金额
         * @param player 目标玩家
         * @return 是否存款成功
         */
        runtime.registerFunction("giveMoney", returns(Type.BOOLEAN).params(Type.NUMBER, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val player = ctx.getPlayerArg(1)
            ctx.setReturnBool(player != null && economy?.depositPlayer(player, amount)?.transactionSuccess() == true)
        }

        /**
         * 设置 sender 的账户余额（通过存取款实现）
         * @param amount 目标余额
         */
        runtime.registerFunction("setMoney", returns(Type.VOID).params(Type.NUMBER)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val player = ctx.getPlayerArg(-1) ?: return@registerFunction
            setBalance(player, amount)
        }

        /**
         * 设置指定玩家的账户余额
         * @param amount 目标余额
         * @param player 目标玩家
         */
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
