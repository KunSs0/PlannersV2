package com.gitee.planners.module.action

import org.bukkit.Bukkit
import taboolib.platform.BukkitPlugin

/**
 * 基于 Bukkit 主线程调度器的动作调度实现。
 *
 * 动作会读写实体、背包与消息，全部运行在主线程；延迟与周期都按 tick 计。
 */
object BukkitActionDispatcher {

    /**
     * 安装调度器，供插件启用阶段调用。
     */
    fun install() {
        ActionScheduler.install { delayTicks, periodTicks, task ->
            val plugin = BukkitPlugin.getInstance()
            if (periodTicks <= 0L) {
                val handle = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks)
                return@install ActionTask { handle.cancel() }
            }
            val handle = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks)
            return@install ActionTask { handle.cancel() }
        }
    }

    /**
     * 卸载调度器，供插件停用阶段调用。
     */
    fun uninstall() {
        ActionScheduler.uninstall()
    }
}
