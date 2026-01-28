package com.gitee.planners.module.fluxon.economy

import com.gitee.planners.module.fluxon.FluxonScriptCache
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import org.tabooproject.fluxon.runtime.java.Export
import org.tabooproject.fluxon.runtime.java.Optional
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 经济系统扩展 (Vault Economy)
 */
object EconomyExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime
        runtime.registerFunction("pl:economy", "economy", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
            ctx.setReturnRef(EconomyObject)
        }
        runtime.exportRegistry.registerClass(EconomyObject::class.java, "pl:economy")
    }

    object EconomyObject {

        @JvmField
        val TYPE: Type = Type.fromClass(EconomyObject::class.java)

        private val economy: Economy? by lazy {
            try {
                Bukkit.getServer().servicesManager.getRegistration(Economy::class.java)?.provider
            } catch (e: Exception) {
                null
            }
        }

        @Export
        fun getBalance(@Optional player: Player): Double {
            return economy?.getBalance(player) ?: 0.0
        }

        @Export
        fun take(amount: Double, @Optional player: Player): Boolean {
            return economy?.withdrawPlayer(player, amount)?.transactionSuccess() ?: false
        }

        @Export
        fun give(amount: Double, @Optional player: Player): Boolean {
            return economy?.depositPlayer(player, amount)?.transactionSuccess() ?: false
        }

        @Export
        fun set(amount: Double, @Optional player: Player) {
            val eco = economy ?: return
            val current = eco.getBalance(player)
            if (current > amount) {
                eco.withdrawPlayer(player, current - amount)
            } else {
                eco.depositPlayer(player, amount - current)
            }
        }
    }
}
