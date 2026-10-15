package com.gitee.planners.core.skill.timing

import java.util.UUID

/**
 * 单次施法等待队列；调度器由宿主显式注入，所有操作由主线程串行执行。
 *
 * @param schedule 按tick延迟执行任务并返回取消句柄的宿主操作。
 */
class SkillCastingQueue(private val schedule: (Int, Runnable) -> Runnable) {
    private val entries = HashMap<UUID, Entry>()

    /**
     * 查询玩家是否仍在等待施法完成。
     *
     * @param playerId 玩家UUID。
     * @return 当前存在未完成会话时返回true。
     */
    fun contains(playerId: UUID): Boolean {
        // 会话仅表示实际等待，不代表冷却或动作动画状态。
        return entries.containsKey(playerId)
    }

    /**
     * 安排一次施法续体，同一玩家已有会话时拒绝重复开始。
     *
     * @param playerId 玩家UUID。
     * @param ticks 已应用急速和取整后的正整数tick数。
     * @param continuation 施法完成后唯一可执行的续体。
     * @return 会话成功建立时返回true。
     * @throws IllegalArgumentException 延迟不为正数时抛出。
     */
    fun start(playerId: UUID, ticks: Int, continuation: Runnable): Boolean {
        // 零时间由调用方直接执行，避免为瞬发技能额外插入一个tick。
        require(ticks > 0) { "施法等待必须为正整数tick。" }
        if (entries.containsKey(playerId)) {
            return false
        }
        val entry = Entry(continuation)
        entries[playerId] = entry
        try {
            entry.cancelTask = schedule(ticks, Runnable {
                // 即使旧任务在取消后被调度，也不能恢复新会话或重复执行资源消耗。
                if (entries[playerId] === entry) {
                    entries.remove(playerId)
                    entry.continuation.run()
                }
            })
        } catch (failure: Throwable) {
            entries.remove(playerId, entry)
            throw failure
        }
        return true
    }

    /**
     * 取消指定玩家的等待，之后到达的旧回调也不得恢复释放。
     *
     * @param playerId 玩家UUID。
     */
    fun cancel(playerId: UUID) {
        // 先移除会话身份，再取消任务，避免宿主取消过程重入。
        val entry = entries.remove(playerId)
        if (entry != null) {
            val task = entry.cancelTask
            if (task != null) {
                task.run()
            }
        }
    }

    /**
     * 在宿主停止时清理全部等待和调度引用。
     */
    fun clear() {
        // 使用快照取消，不在遍历活动映射时删除元素。
        val players = ArrayList(entries.keys)
        for (playerId in players) {
            cancel(playerId)
        }
    }

    /**
     * 一个等待会话的续体及取消句柄。
     *
     * @property continuation 成功完成时执行的唯一续体。
     */
    private class Entry(val continuation: Runnable) {
        /**
         * 宿主调度成功后写入的取消句柄。
         */
        var cancelTask: Runnable? = null
    }
}
