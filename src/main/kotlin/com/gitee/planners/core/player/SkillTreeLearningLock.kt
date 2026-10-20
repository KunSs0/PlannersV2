package com.gitee.planners.core.player

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 同一玩家技能树学习请求的排他锁，覆盖不同节点和职业阶段的共享点数。
 */
object SkillTreeLearningLock {
    private val players = ConcurrentHashMap.newKeySet<UUID>()

    /**
     * 在准备外部条件之前取得请求所有权。
     *
     * @param playerId 发起学习的玩家。
     * @return 没有其他未完成学习请求时返回 true。
     */
    fun acquire(playerId: UUID): Boolean {
        // 原子加入保证不同互斥节点不能并发进入消费流程。
        return players.add(playerId)
    }

    /**
     * 在请求成功或失败后释放所有权。
     *
     * @param playerId 本次请求的玩家。
     */
    fun release(playerId: UUID) {
        // 仅由成功取得锁的请求调用，避免失败重入释放其他请求。
        players.remove(playerId)
    }
}
