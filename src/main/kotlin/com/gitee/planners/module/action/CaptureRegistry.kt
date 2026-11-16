package com.gitee.planners.module.action

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

/**
 * 全局会话捕捉注册表。
 *
 * 负责登记、销毁与按捕捉者查找；同一捕捉者的同名捕捉只保留最新一份（新一次捕捉覆盖旧槽），
 * 过期与失效采用惰性清理。
 */
object CaptureRegistry {

    /** 实例表，按实例 ID 索引。 */
    private val sessions = LinkedHashMap<UUID, CaptureSession>()

    /**
     * 判断捕捉者是否仍有效的入口；单元测试可替换。
     */
    var ownerValid: (UUID) -> Boolean = { ownerId -> isPlayerOnline(ownerId) }

    /**
     * 登记一个实例，同名槽覆盖旧的。
     *
     * @param session 新实例。
     */
    fun register(session: CaptureSession) {
        purge()
        stopOwned(session.ownerId, session.id)
        sessions[session.instance] = session
    }

    /**
     * 按实例 ID 移除。
     *
     * @param instanceId 实例 ID。
     * @return 移除成功时返回 true。
     */
    fun remove(instanceId: UUID): Boolean {
        val session = sessions.remove(instanceId)
        if (session == null) {
            return false
        }
        session.cancelTask()
        return true
    }

    /**
     * 结束捕捉者名下指定分类的实例。
     *
     * @param ownerId 捕捉者唯一 ID。
     * @param id 业务分类名。
     * @return 结束的实例数量。
     */
    fun stopOwned(ownerId: UUID, id: String): Int {
        var removed = 0
        val iterator = sessions.values.iterator()
        while (iterator.hasNext()) {
            val session = iterator.next()
            if (session.ownerId != ownerId || session.id != id) {
                continue
            }
            iterator.remove()
            session.cancelTask()
            removed += 1
        }
        return removed
    }

    /**
     * 按捕捉者与分类查找实例。
     *
     * @param ownerId 捕捉者唯一 ID。
     * @param id 业务分类名。
     * @return 实例；不存在时返回 null。
     */
    fun find(ownerId: UUID, id: String): CaptureSession? {
        purge()
        for (session in sessions.values) {
            if (session.ownerId == ownerId && session.id == id) {
                return session
            }
        }
        return null
    }

    /**
     * 清空全部实例。
     */
    fun clear() {
        for (session in sessions.values) {
            session.cancelTask()
        }
        sessions.clear()
    }

    /**
     * 清理已到期或捕捉者已离线的实例。
     */
    fun purge() {
        val now = System.currentTimeMillis()
        val iterator = sessions.values.iterator()
        while (iterator.hasNext()) {
            val session = iterator.next()
            if (session.isExpired(now) || !ownerValid(session.ownerId)) {
                iterator.remove()
                session.cancelTask()
            }
        }
    }

    /**
     * 查询玩家是否在线。
     *
     * @param ownerId 玩家唯一 ID。
     * @return 在线时返回 true。
     */
    private fun isPlayerOnline(ownerId: UUID): Boolean {
        val player: Player? = Bukkit.getPlayer(ownerId)
        if (player == null) {
            return false
        }
        return player.isOnline
    }
}
