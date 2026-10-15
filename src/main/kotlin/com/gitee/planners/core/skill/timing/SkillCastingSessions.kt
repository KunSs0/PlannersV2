package com.gitee.planners.core.skill.timing

import com.gitee.planners.core.skill.ExecutableResult
import com.gitee.planners.api.event.PluginReloadEvents
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit

/**
 * Bukkit主线程上的施法等待宿主；不会修改任何招式动画播放速度。
 */
object SkillCastingSessions {
    private val queue = SkillCastingQueue { ticks, action ->
        val task = submit(delay = ticks.toLong()) {
            action.run()
        }
        Runnable {
            task.cancel()
        }
    }

    /**
     * 检查玩家是否已有未完成的施法等待。
     *
     * @param player 当前玩家。
     * @return 玩家正在施法时返回true。
     */
    fun isCasting(player: Player): Boolean {
        // 本状态只用于拒绝等待期间的重复释放。
        check(Bukkit.isPrimaryThread()) { "施法会话只能在Bukkit主线程访问。" }
        return queue.contains(player.uniqueId)
    }

    /**
     * 等待最终施法tick后恢复既有释放流程，瞬发技能直接执行。
     *
     * @param player 当前施放者。
     * @param ticks 本次时间快照的最终施法tick。
     * @param continuation 读条完成后进入Hook与最终校验的续体。
     * @return 瞬发的实际结果或表示正在等待的拦截结果。
     */
    fun begin(player: Player, ticks: Int, continuation: () -> ExecutableResult): ExecutableResult {
        // 所有资源仍在原有最终校验后消耗，离线、死亡或取消等待不会提前扣除。
        check(Bukkit.isPrimaryThread()) { "施法会话只能在Bukkit主线程创建。" }
        require(ticks >= 0) { "施法tick不能为负数。" }
        if (queue.contains(player.uniqueId)) {
            return ExecutableResult.cancelledWithEvent()
        }
        if (ticks == 0) {
            return continuation()
        }
        val started = queue.start(player.uniqueId, ticks, Runnable {
            if (player.isOnline && !player.isDead) {
                continuation()
            }
        })
        if (!started) {
            return ExecutableResult.cancelledWithEvent()
        }
        return ExecutableResult.intercepted("casting")
    }

    /**
     * 供明确的技能中断行为取消等待。
     *
     * @param player 需要中断的玩家。
     */
    @JvmStatic
    fun cancel(player: Player) {
        // 不自动推导受伤或移动中断规则；业务可在明确的事件中调用此入口。
        check(Bukkit.isPrimaryThread()) { "施法会话只能在Bukkit主线程取消。" }
        queue.cancel(player.uniqueId)
    }

    /**
     * 玩家退出时取消等待，防止旧对象继续释放。
     *
     * @param event 玩家退出事件。
     */
    @SubscribeEvent
    fun onQuit(event: PlayerQuitEvent) {
        // 生命周期结束必须释放调度引用。
        cancel(event.player)
    }

    /**
     * 玩家死亡时取消尚未完成的施法。
     *
     * @param event 玩家死亡事件。
     */
    @SubscribeEvent
    fun onDeath(event: PlayerDeathEvent) {
        // 复活后也不能恢复死亡前捕获的技能续体。
        cancel(event.entity)
    }

    /**
     * 配置重载前取消旧代际捕获的技能续体。
     *
     * @param event Planners重载前事件。
     */
    @SubscribeEvent
    fun onReload(event: PluginReloadEvents.Pre) {
        // 防止旧技能模块在新Workspace激活后提交旧资源或执行旧action。
        cancelAll()
    }

    /**
     * 配置或属性Provider代际结束时取消全部尚未完成的施法。
     */
    fun cancelAll() {
        // 同步清理唯一会话身份，确保旧任务即使到达也不能恢复。
        queue.clear()
    }

    /**
     * 插件停止时移除全部施法会话。
     */
    @Awake(LifeCycle.DISABLE)
    fun onDisable() {
        // 清除身份后取消任务，迟到的回调不能执行任何业务。
        cancelAll()
    }
}
