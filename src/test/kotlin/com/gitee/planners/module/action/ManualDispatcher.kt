package com.gitee.planners.module.action

/**
 * 测试用调度器，按调用顺序记录任务，由用例手动推进时间。
 *
 * 不依赖 Bukkit 调度器，因此可以在普通单元测试里验证定时器注册、执行与取消。
 */
class ManualDispatcher : ActionScheduler.Dispatcher {

    /** 已安排的任务，按创建顺序。 */
    val tasks = ArrayList<ManualTask>()

    override fun schedule(delayTicks: Long, periodTicks: Long, task: Runnable): ActionTask {
        val created = ManualTask(delayTicks, periodTicks, task)
        tasks.add(created)
        return created
    }

    /** 推进一次，触发全部未取消任务。 */
    fun tick() {
        val snapshot = ArrayList(tasks)
        for (task in snapshot) {
            task.fire()
        }
    }

    /**
     * 单个可手动推进的任务。
     *
     * @property delayTicks 首次延迟 tick。
     * @property periodTicks 重复周期 tick；0 表示单次。
     * @property runnable 到期动作。
     */
    class ManualTask(
        val delayTicks: Long,
        val periodTicks: Long,
        val runnable: Runnable,
    ) : ActionTask {

        /** 是否已被取消。 */
        var cancelled = false

        /** 已执行次数。 */
        var fired = 0

        override fun cancel() {
            cancelled = true
        }

        /** 若未取消则执行一次。 */
        fun fire() {
            if (cancelled) {
                return
            }
            fired += 1
            runnable.run()
        }
    }
}
