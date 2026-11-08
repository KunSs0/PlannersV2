package com.gitee.planners.module.currency

import com.gitee.haidercore.module.economy.api.EconomyAPI
import com.gitee.haidercore.module.economy.common.value.Money
import org.bukkit.entity.Player
import taboolib.common.platform.function.warning
import taboolib.library.configuration.ConfigurationSection
import java.math.BigDecimal

/**
 * 由 HaiderCore 经济服务支撑的货币实现。
 *
 * 货币定义只声明 `currency-id`，读写都走 [EconomyAPI]；
 * 不再为每种货币维护一段脚本，也不引入脚本语言依赖。
 *
 * @property root 货币配置节。
 */
class OpenConvertibleCurrencyImpl(val root: ConfigurationSection) : OpenConvertibleCurrency {

    override val id = root.name

    override val name = root.getString("name")!!

    /** 经济服务里登记的货币标识；缺省时退化为货币名。 */
    private val currencyId = root.getString("currency-id") ?: id

    override fun get(player: Player): Double {
        if (!ensureEnabled()) {
            return 0.0
        }
        return EconomyAPI.getBalance(player.uniqueId, currencyId).value.toDouble()
    }

    override fun give(player: Player, amount: Double): Boolean {
        if (!ensureEnabled()) {
            return false
        }
        return EconomyAPI.deposit(player.uniqueId, currencyId, money(amount)).success
    }

    override fun take(player: Player, amount: Double): Boolean {
        if (!ensureEnabled()) {
            return false
        }
        if (get(player) < amount) {
            return false
        }
        return EconomyAPI.withdraw(player.uniqueId, currencyId, money(amount)).success
    }

    override fun set(player: Player, amount: Double) {
        if (!ensureEnabled()) {
            return
        }
        EconomyAPI.setBalance(player.uniqueId, currencyId, money(amount))
    }

    override fun has(player: Player, amount: Double): Boolean {
        return get(player) >= amount
    }

    /**
     * 检查经济服务是否可用。
     *
     * @return 可用时返回 true；否则记录一次告警并返回 false。
     */
    private fun ensureEnabled(): Boolean {
        if (!EconomyAPI.isEnabled()) {
            warning("Currency $id requires HaiderCore economy, but the service is not enabled.")
            return false
        }
        return true
    }

    /**
     * 把数值转换为经济服务的精确货币值。
     *
     * @param amount 货币数量。
     * @return 精确货币值。
     */
    private fun money(amount: Double): Money {
        return Money.ofDecimal(BigDecimal.valueOf(amount))
    }
}
