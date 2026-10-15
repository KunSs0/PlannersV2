package com.gitee.planners.core.condition

import org.bukkit.Bukkit
import taboolib.platform.util.bukkitPlugin
import java.util.concurrent.Executor

/** 学习条件异步准备后的主线程执行器，保证校验、消耗与节点变更在同一线程执行。 */
object ConditionMainThread : Executor {
    /** 已在主线程时直接运行，否则只提交调度，不阻塞调用线程。 */
    override fun execute(command: Runnable) {
        if (Bukkit.isPrimaryThread()) {
            command.run()
        } else {
            Bukkit.getScheduler().runTask(bukkitPlugin, command)
        }
    }
}
