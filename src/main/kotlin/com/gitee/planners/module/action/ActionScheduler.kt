package com.gitee.planners.module.action

/**
 * 动作延迟与定时调度入口。
 *
 * 运行时只通过本入口安排任务；插件启用阶段安装实际实现，
 * 重载或停用时递增代际并取消已安排任务，使尚未执行的旧任务失效。
 */
object ActionScheduler {

    /**
     * 实际调度实现。
     */
    fun interface Dispatcher {

        /**
         * 安排一个任务。
         *
         * @param delayTicks 首次延迟 tick，0 表示下一 tick。
         * @param periodTicks 重复周期 tick；0 表示只执行一次。
         * @param task 待执行任务。
         * @return 可用于取消的句柄。
         */
        fun schedule(delayTicks: Long, periodTicks: Long, task: Runnable): ActionTask
    }

    private var dispatcher: Dispatcher? = null

    private var generation = 0L

    private val scheduled = ArrayList<ActionTask>()

    /**
     * 安装实际调度器。
     *
     * @param value 调度器实现。
     */
    fun install(value: Dispatcher) {
        dispatcher = value
    }

    /**
     * 卸载调度器并取消全部已安排任务，供插件停用使用。
     */
    fun uninstall() {
        cancelAll()
        dispatcher = null
    }

    /**
     * 判断是否已安装调度器。
     *
     * @return 已安装时返回 true。
     */
    fun isInstalled(): Boolean {
        return dispatcher != null
    }

    /**
     * 获取当前代际。
     *
     * @return 当前代际。
     */
    fun currentGeneration(): Long {
        return generation
    }

    /**
     * 递增代际，使尚未执行的旧任务失效。
     *
     * @return 新代际。
     */
    fun nextGeneration(): Long {
        generation += 1
        cancelAll()
        return generation
    }

    /**
     * 安排一次延迟动作。
     *
     * @param delayTicks 延迟 tick。
     * @param expectedGeneration 创建任务时的代际。
     * @param action 延迟动作。
     * @return 任务句柄。
     */
    fun schedule(delayTicks: Long, expectedGeneration: Long, action: Runnable): ActionTask {
        return submit(delayTicks, 0L, expectedGeneration, action)
    }

    /**
     * 安排一个重复动作。
     *
     * @param delayTicks 首次延迟 tick。
     * @param periodTicks 重复周期 tick，必须为正数。
     * @param expectedGeneration 创建任务时的代际。
     * @param action 每次到期执行的动作。
     * @return 任务句柄。
     */
    fun scheduleRepeating(
        delayTicks: Long,
        periodTicks: Long,
        expectedGeneration: Long,
        action: Runnable,
    ): ActionTask {
        if (periodTicks <= 0L) {
            throw IllegalArgumentException("Repeating action requires a positive period.")
        }
        return submit(delayTicks, periodTicks, expectedGeneration, action)
    }

    /**
     * 取消全部已安排任务。
     */
    fun cancelAll() {
        val snapshot = ArrayList(scheduled)
        scheduled.clear()
        for (task in snapshot) {
            task.cancel()
        }
    }

    /**
     * 提交任务并登记句柄。
     *
     * @param delayTicks 首次延迟。
     * @param periodTicks 重复周期，0 表示单次。
     * @param expectedGeneration 创建任务时的代际。
     * @param action 到期动作。
     * @return 任务句柄。
     */
    private fun submit(
        delayTicks: Long,
        periodTicks: Long,
        expectedGeneration: Long,
        action: Runnable,
    ): ActionTask {
        val current = dispatcher
        if (current == null) {
            throw IllegalStateException("Planners action scheduler is not installed.")
        }
        var handleRef: ActionTask? = null
        val handle = current.schedule(delayTicks, periodTicks) {
            if (generation == expectedGeneration) {
                action.run()
            }
            // 单次任务执行后立即脱钩，避免登记表随执行次数无限增长。
            if (periodTicks <= 0L) {
                val finished = handleRef
                if (finished != null) {
                    scheduled.remove(finished)
                }
            }
        }
        handleRef = handle
        scheduled.add(handle)
        return handle
    }
}
