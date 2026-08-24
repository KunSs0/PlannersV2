package com.gitee.planners.api.directing

import org.bukkit.entity.Player

/**
 * 指向会话的外部输入入口。
 */
object DirectingAPI {

    @Volatile
    private var receiver: DirectingInputReceiver? = null

    /**
     * 绑定 Planners core 提供的唯一指向输入接收者。
     *
     * @param inputReceiver 待绑定的接收者。
     * @throws IllegalStateException 已有不同接收者时抛出。
     */
    fun bind(inputReceiver: DirectingInputReceiver) {
        val previous = receiver
        if (previous != null && previous !== inputReceiver) {
            throw IllegalStateException("DirectingAPI 已绑定输入接收者")
        }
        receiver = inputReceiver
    }

    /**
     * 解除当前接收者绑定。
     *
     * @param inputReceiver 当前生命周期即将停止的接收者。
     */
    fun unbind(inputReceiver: DirectingInputReceiver) {
        if (receiver === inputReceiver) {
            receiver = null
        }
    }

    /**
     * 向当前玩家的指向会话提交瞄准快照。
     *
     * @param player 输入所属玩家。
     * @param input provider 专属快照。
     */
    fun update(player: Player, input: DirectingInput) {
        resolveReceiver().update(player, input)
    }

    /**
     * 确认当前玩家的指向会话。
     *
     * @param player 输入所属玩家。
     * @param input 松开按键时的 provider 专属快照。
     */
    fun confirm(player: Player, sourceKey: String, input: DirectingInput) {
        resolveReceiver().confirm(player, sourceKey, input)
    }

    /**
     * 取消当前玩家的指向会话。
     *
     * @param player 要取消会话的玩家。
     */
    fun cancel(player: Player) {
        resolveReceiver().cancel(player)
    }

    /**
     * 获取已绑定的核心输入接收者。
     *
     * @return 当前接收者。
     * @throws IllegalStateException Planners core 尚未初始化时抛出。
     */
    private fun resolveReceiver(): DirectingInputReceiver {
        val current = receiver
        if (current == null) {
            throw IllegalStateException("DirectingAPI 尚未绑定输入接收者")
        }
        return current
    }
}
