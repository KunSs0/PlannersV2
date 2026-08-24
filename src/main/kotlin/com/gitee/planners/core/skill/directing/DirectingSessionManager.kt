package com.gitee.planners.core.skill.directing

import com.gitee.planners.api.directing.DirectingInput
import com.gitee.planners.api.directing.DirectingAPI
import com.gitee.planners.api.directing.DirectingInputReceiver
import com.gitee.planners.api.directing.DirectingResult
import com.gitee.planners.api.directing.DirectingSession
import com.gitee.planners.api.directing.DirectingSessionCloseReason
import com.gitee.planners.api.directing.DirectingSessionContext
import com.gitee.planners.api.directing.DirectingSessionFactoryRegistry
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.ExecutableResult
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.common.platform.event.SubscribeEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 指向会话的唯一拥有者。
 *
 * 该管理器负责会话替换、超时、玩家离线清理和确认后恢复技能释放；provider 不直接维护玩家会话表。
 */
object DirectingSessionManager : DirectingInputReceiver {

    private val sessions = ConcurrentHashMap<UUID, Entry>()
    private val nextSessionId = AtomicLong(1L)

    /**
     * 创建一个指向会话。
     *
     * @param player 当前施法者。
     * @param skill 当前玩家技能。
     * @param onConfirmed 服务端校验成功后的释放续体。
     * @return 创建成功时返回 true；未注册运行时创建器时返回 false。
     */
    fun start(player: Player, skill: PlayerSkill, sourceKey: String, onConfirmed: (DirectingResult) -> ExecutableResult): Boolean {
        val definition = skill.immutable.directing
        if (definition == null) {
            return false
        }
        val factory = DirectingSessionFactoryRegistry.getOrNull(definition.type)
        if (factory == null) {
            return false
        }
        cancel(player, DirectingSessionCloseReason.REPLACED)
        val sessionId = nextSessionId.getAndIncrement()
        val context = DirectingSessionContext(sessionId, player, skill, definition, sourceKey)
        val session = factory.create(context)
        val entry = Entry(sessionId, definition.type, sourceKey, session, onConfirmed)
        val previous = sessions.put(player.uniqueId, entry)
        if (previous != null) {
            previous.close(DirectingSessionCloseReason.REPLACED)
        }
        entry.timeoutTask = submit(delay = definition.maxDurationTicks.toLong()) {
            timeout(player.uniqueId, entry)
        }
        try {
            session.begin()
        } catch (throwable: Throwable) {
            sessions.remove(player.uniqueId, entry)
            entry.close(DirectingSessionCloseReason.CANCELLED)
            throw throwable
        }
        return true
    }

    /**
     * 更新当前会话的瞄准快照。
     *
     * @param player 输入所属玩家。
     * @param input provider 专属输入。
     */
    override fun update(player: Player, input: DirectingInput) {
        val entry = sessions[player.uniqueId]
        if (entry == null || entry.sessionId != input.sessionId || entry.type != input.type) {
            return
        }
        entry.session.update(input)
    }

    /**
     * 以最终快照确认会话。
     *
     * @param player 输入所属玩家。
     * @param input provider 专属输入。
     */
    override fun confirm(player: Player, sourceKey: String, input: DirectingInput) {
        val entry = sessions[player.uniqueId]
        if (entry == null || entry.sessionId != input.sessionId || entry.type != input.type || entry.sourceKey != sourceKey) {
            return
        }
        val removed = sessions.remove(player.uniqueId, entry)
        if (!removed) {
            return
        }
        entry.cancelTimeout()
        val result: DirectingResult?
        try {
            result = entry.session.confirm(input)
        } catch (throwable: Throwable) {
            entry.close(DirectingSessionCloseReason.CANCELLED)
            throw throwable
        }
        if (result == null) {
            entry.close(DirectingSessionCloseReason.CANCELLED)
            return
        }
        entry.close(DirectingSessionCloseReason.CONFIRMED)
        entry.onConfirmed(result)
    }

    /**
     * 按原因取消玩家当前会话。
     *
     * @param player 要清理的玩家。
     * @param reason 停止原因。
     */
    fun cancel(player: Player, reason: DirectingSessionCloseReason) {
        cancel(player.uniqueId, reason)
    }

    /**
     * 取消当前玩家的指向会话。
     *
     * @param player 要取消会话的玩家。
     */
    override fun cancel(player: Player) {
        cancel(player, DirectingSessionCloseReason.CANCELLED)
    }

    /**
     * 在 Planners 核心启用时绑定公开输入入口。
     */
    @Awake(LifeCycle.ENABLE)
    fun onEnable() {
        DirectingAPI.bind(DirectingSessionManager)
    }

    /**
     * 玩家退出时停止其正在进行的指向会话。
     *
     * @param event 离线事件。
     */
    @SubscribeEvent
    fun onPlayerQuit(event: PlayerQuitEvent) {
        cancel(event.player.uniqueId, DirectingSessionCloseReason.PLAYER_QUIT)
    }

    /**
     * 插件停止时释放全部 provider 临时资源。
     */
    @Awake(LifeCycle.DISABLE)
    fun onDisable() {
        val entries = ArrayList(sessions.entries)
        sessions.clear()
        for (mapEntry in entries) {
            mapEntry.value.close(DirectingSessionCloseReason.PLUGIN_DISABLED)
        }
        DirectingAPI.unbind(DirectingSessionManager)
    }

    /**
     * 处理指定会话的超时任务。
     *
     * @param playerId 玩家唯一标识。
     * @param entry 创建任务时绑定的会话条目。
     */
    private fun timeout(playerId: UUID, entry: Entry) {
        val removed = sessions.remove(playerId, entry)
        if (!removed) {
            return
        }
        entry.close(DirectingSessionCloseReason.TIMED_OUT)
    }

    /**
     * 按 UUID 移除单个会话。
     *
     * @param playerId 玩家唯一标识。
     * @param reason 停止原因。
     */
    private fun cancel(playerId: UUID, reason: DirectingSessionCloseReason) {
        val entry = sessions.remove(playerId)
        if (entry == null) {
            return
        }
        entry.close(reason)
    }

    /**
     * 单个会话与其释放续体。
     */
    private class Entry(val sessionId: Long, val type: String, val sourceKey: String, val session: DirectingSession, val onConfirmed: (DirectingResult) -> ExecutableResult) {

        var timeoutTask: PlatformExecutor.PlatformTask? = null

        /**
         * 取消会话超时任务。
         */
        fun cancelTimeout() {
            val task = timeoutTask
            if (task != null) {
                task.cancel()
            }
            timeoutTask = null
        }

        /**
         * 停止任务并通知 provider 会话。
         *
         * @param reason 会话停止原因。
         */
        fun close(reason: DirectingSessionCloseReason) {
            cancelTimeout()
            session.close(reason)
        }
    }
}
