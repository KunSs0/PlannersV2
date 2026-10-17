package com.gitee.planners.module.compat.attribute

import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.job.target.asTarget
import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.api.event.player.PlayerLevelChangedEvent
import com.gitee.planners.api.event.player.PlayerProfileLoadedEvent
import com.gitee.planners.api.event.player.PlayerSetRouteEvent
import com.gitee.planners.api.event.player.PlayerSkillEvent
import com.gitee.planners.api.event.player.PlayerSkillTreeNodeEvent
import com.gitee.planners.core.attribute.AttributeProxy
import com.gitee.planners.core.player.PlayerTemplate
import org.bukkit.Bukkit
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import java.util.concurrent.atomic.AtomicLong

/**
 * 在主线程刷新就绪档案，以代际和档案身份拒绝旧任务。
 */
object AttributeProvider {

    private val generation = AtomicLong()

    @Volatile
    private var reloading = false

    /**
     * 使已提交任务失效，重载失败时也不允许旧配置写回。
     */
    fun invalidate() {
        // 原子代际允许异步档案事件与主线程重载交错提交。
        generation.incrementAndGet()
    }

    /**
     * 统一提交驱动写操作，主线程直接执行，异步调用排队并检查代际。
     *
     * @param driver 提交时的驱动实例。
     * @param action 仅调用外部属性 API 的写操作。
     */
    fun dispatch(driver: AttributeDriver, action: () -> Unit) {
        // 对所有驱动实施同一线程协议，不把调度逻辑下放到具体适配器。
        if (reloading) {
            return
        }
        val expectedGeneration = generation.get()
        submit(now = Bukkit.isPrimaryThread()) {
            if (expectedGeneration != generation.get() || AttributeDriver.current() !== driver) {
                return@submit
            }
            action()
        }
    }

    /**
     * 在驱动注销时统一清除在线玩家的职业代理来源。
     *
     * @param driver 已失效的输出驱动，清理不再通过当前驱动查找。
     */
    fun clear(driver: AttributeDriver) {
        check(Bukkit.isPrimaryThread()) { "属性驱动注销必须在 Bukkit 主线程执行" }
        // 来源 ID 固定，无需每个外部驱动另外跟踪实体和来源集合。
        for (player in Bukkit.getOnlinePlayers()) {
            driver.remove(player.asTarget(), "planners-proxy")
        }
    }

    /**
     * Planners 自身关闭时失效任务并按同一协议注销当前驱动。
     */
    @Awake(LifeCycle.DISABLE)
    fun shutdown() {
        // 与外部插件注销共用入口，确保卸载顺序不会遗留职业来源。
        invalidate()
        val driver = AttributeDriver.current()
        if (driver != null) {
            AttributeDriver.unregister(driver)
        }
    }

    /**
     * 在主线程读取提交完成后的最新状态并同步属性。
     *
     * @param template 触发刷新时已就绪的玩家档案。
     */
    fun refresh(template: PlayerTemplate) {
        // 重载中与失败重载后不执行半套配置，成功 Post 才恢复接收。
        if (reloading) {
            return
        }
        val expectedGeneration = generation.get()
        submit {
            // 档案替换、退出或热重载后，禁止旧事件重新添加职业来源。
            val player = template.onlinePlayer
            if (expectedGeneration != generation.get() || !player.isOnline) {
                return@submit
            }
            if (PlayerTemplateAPI.getOrNull(player.uniqueId) !== template) {
                return@submit
            }
            AttributeProxy.sync(player)
        }
    }

    /**
     * 注册驱动或成功重载后刷新在线且档案就绪的玩家。
     */
    fun refreshOnline() {
        val expectedGeneration = generation.get()
        submit {
            // 在线列表与档案查询在主线程完成。
            if (expectedGeneration != generation.get()) {
                return@submit
            }
            for (player in Bukkit.getOnlinePlayers()) {
                val template = PlayerTemplateAPI.getOrNull(player.uniqueId)
                if (template != null) {
                    refresh(template)
                }
            }
        }
    }

    /**
     * 将异步档案加载事件交给主线程。
     *
     * @param e 已就绪档案事件。
     */
    @SubscribeEvent
    fun e(e: PlayerProfileLoadedEvent) {
        refresh(e.template)
    }

    /**
     * 排队后读取技能提交完成后的实际等级。
     *
     * @param e 技能等级变更事件。
     */
    @SubscribeEvent
    fun e(e: PlayerSkillEvent.LevelChange) {
        refresh(e.template)
    }

    /**
     * 节点变化后重算同一属性来源。
     *
     * @param e 技能树节点变更事件。
     */
    @SubscribeEvent
    fun e(e: PlayerSkillTreeNodeEvent) {
        refresh(e.template)
    }

    /**
     * 转职提交后重算当前职业。
     *
     * @param e 转职完成事件。
     */
    @SubscribeEvent
    fun e(e: PlayerSetRouteEvent.Post) {
        refresh(e.template)
    }

    /**
     * 只接收等级提交完成通知，避免读取修改前的旧等级。
     *
     * @param e 实际等级已写入事件。
     */
    @SubscribeEvent
    fun e(e: PlayerLevelChangedEvent) {
        refresh(e.template)
    }

    /**
     * 重载开始立即失效旧任务。
     *
     * @param e 重载开始事件。
     */
    @SubscribeEvent
    fun e(e: PluginReloadEvents.Pre) {
        reloading = true
        invalidate()
    }

    /**
     * 配置与脚本成功重载后同步在线档案。
     *
     * @param e 重载完成事件。
     */
    @SubscribeEvent
    fun e(e: PluginReloadEvents.Post) {
        reloading = false
        refreshOnline()
    }
}
