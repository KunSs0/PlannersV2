package com.gitee.planners.module.action

import com.gitee.action.api.spi.HostServices
import com.gitee.action.api.spi.ScheduledHandle
import org.bukkit.plugin.Plugin

/**
 * 技能动作的宿主服务。
 *
 * 延迟任务沿用技能侧原有的调度器与代际语义：重载或停服后旧续体在真正执行前失效。
 * 技能会话不因玩家离线而中断（与原行为一致），因此活性只受代际约束。
 *
 * @property plugin 调度与日志使用的插件实例
 */
class PlannersHostServices(private val plugin: Plugin) : HostServices {

    override fun schedule(delayTicks: Long, task: Runnable): ScheduledHandle {
        val generation = ActionScheduler.currentGeneration()
        val scheduled = ActionScheduler.schedule(delayTicks, generation, Runnable { task.run() })
        return ScheduledHandle { scheduled.cancel() }
    }

    override fun isActive(): Boolean {
        // 代际校验由调度器在续体执行前完成，这里不再重复判断
        return true
    }

    override fun warn(path: String, message: String) {
        plugin.logger.warning(path + " " + message)
    }
}
