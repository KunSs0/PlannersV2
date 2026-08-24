package com.gitee.planners.api.directing

import org.bukkit.entity.Player

/**
 * 指向性输入的运行时接收契约。
 *
 * Planners core 实现并绑定此契约；外部网络层只能通过 [DirectingAPI] 提交输入。
 */
interface DirectingInputReceiver {

    /**
     * 接收瞄准期间的快照。
     *
     * @param player 输入所属玩家。
     * @param input provider 专属快照。
     */
    fun update(player: Player, input: DirectingInput)

    /**
     * 确认当前指向会话。
     *
     * @param player 输入所属玩家。
     * @param sourceKey 松开按键标识。
     * @param input 最终 provider 专属快照。
     */
    fun confirm(player: Player, sourceKey: String, input: DirectingInput)

    /**
     * 取消当前玩家的指向会话。
     *
     * @param player 要取消会话的玩家。
     */
    fun cancel(player: Player)
}
