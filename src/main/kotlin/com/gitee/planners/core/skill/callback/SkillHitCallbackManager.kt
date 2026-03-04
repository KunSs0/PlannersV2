package com.gitee.planners.core.skill.callback

import com.gitee.planners.api.event.player.PlayerDamageEntityEvent
import com.gitee.planners.module.script.JsSession
import org.bukkit.entity.Player
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 技能命中回调管理器
 *
 * 当技能脚本定义 handleHit() 函数时，释放技能后该函数将在玩家命中实体时被调用。
 *
 * 使用示例（JS 脚本）：
 * ```js
 * function main() {
 *   // 技能释放逻辑
 * }
 * function handleHit() {
 *   // 命中时执行，__arg0 为被命中的 Entity
 * }
 * ```
 */
object SkillHitCallbackManager {

    private data class Entry(val session: JsSession, val expireTime: Long)

    private val sessions = ConcurrentHashMap<UUID, Entry>()

    /** 默认回调有效期（毫秒） */
    private const val DEFAULT_TTL_MS = 30_000L

    /**
     * 注册命中回调。若 session 未定义 handleHit 函数则直接关闭 session。
     */
    fun register(player: Player, session: JsSession, ttlMs: Long = DEFAULT_TTL_MS) {
        if (session.hasFunction("handleHit")) {
            // 替换旧 session
            sessions.remove(player.uniqueId)?.session?.runCatching { close() }
            sessions[player.uniqueId] = Entry(session, System.currentTimeMillis() + ttlMs)
        } else {
            session.runCatching { close() }
        }
    }

    /** 手动注销并关闭 session */
    fun unregister(player: Player) {
        sessions.remove(player.uniqueId)?.session?.runCatching { close() }
    }

    @SubscribeEvent
    fun onDamage(event: PlayerDamageEntityEvent) {
        val entry = sessions[event.player.uniqueId] ?: return
        if (System.currentTimeMillis() > entry.expireTime) {
            sessions.remove(event.player.uniqueId)?.session?.runCatching { close() }
            return
        }
        runCatching {
            entry.session.invokeFunction("handleHit", event.entity)
        }
    }
}
