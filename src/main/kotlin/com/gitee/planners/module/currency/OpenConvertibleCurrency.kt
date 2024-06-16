package com.gitee.planners.module.currency

import com.gitee.planners.api.common.registry.Unique
import org.bukkit.entity.Player

interface OpenConvertibleCurrency : Unique {

    val name: String

    /**
     * 从玩家身上扣除货币
     * @param player 玩家
     * @param amount 金额
     * @return 是否扣除成功
     */
    fun take(player: Player, amount: Double): Boolean

    /**
     * 给予玩家货币
     * @param player 玩家
     * @param amount 金额
     * @return 是否给予成功
     */
    fun give(player: Player, amount: Double): Boolean

    /**
     * 获取玩家身上的货币
     * @param player 玩家
     * @return 金额
     */
    fun get(player: Player): Double

    /**
     * 设置玩家身上的货币
     * @param player 玩家
     * @param amount 金额
     */
    fun set(player: Player, amount: Double)

    /**
     * 玩家是否有足够的货币
     * @param player 玩家
     * @param amount 金额
     * @return 是否有足够的货币
     */
    fun has(player: Player, amount: Double): Boolean

}
