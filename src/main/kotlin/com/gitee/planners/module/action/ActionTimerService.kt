package com.gitee.planners.module.action

import taboolib.common.platform.function.warning

/**
 * 定时触发器服务。
 *
 * 加载期扫描全部结构化技能的 `~onTimer` 行，为每行注册一个重复任务；
 * 重载时整体取消后重建，因此不存在跨代际的幽灵定时器。
 *
 * 定时器属于技能而不是某个玩家，执行上下文没有施法者；需要玩家数据的动作
 * 会在目标解析阶段被跳过。
 */
object ActionTimerService {

    private val tasks = ArrayList<ActionTask>()

    /**
     * 取消全部定时任务。
     */
    fun stop() {
        val snapshot = ArrayList(tasks)
        tasks.clear()
        for (task in snapshot) {
            task.cancel()
        }
    }

    /**
     * 按当前技能结构重建全部定时任务。
     *
     * @param specs 已编译的技能结构。
     */
    fun restart(specs: Collection<ActionSpec>) {
        stop()
        if (!ActionScheduler.isInstalled()) {
            warning("Action timer service skipped: scheduler is not installed.")
            return
        }
        val generation = ActionScheduler.currentGeneration()
        for (spec in specs) {
            for (flow in spec.flows.values) {
                for (line in flow.actions) {
                    if (!line.isTimer()) {
                        continue
                    }
                    startTimer(spec, flow, line, generation)
                }
            }
        }
    }

    /**
     * 注册单条定时器行。
     *
     * @param spec 所属技能结构。
     * @param flow 该行所属流程。
     * @param line 定时器行。
     * @param generation 当前调度代际。
     */
    private fun startTimer(spec: ActionSpec, flow: ActionFlow, line: ActionLine, generation: Long) {
        val arguments = line.triggerArguments
        val trigger = line.trigger
        if (arguments == null || trigger == null) {
            return
        }
        val period = trigger.interval(arguments)
        if (period == null) {
            warning("Action timer ${line.path} has no literal period and was skipped.")
            return
        }
        val delay = trigger.delay(arguments).toLong()
        val times = trigger.times(arguments)
        val counter = RepeatCounter(times)
        val scope = ActionScopeFactory.forSystem("skill:" + spec.skillId + ".timer")
        val handle = ActionScheduler.scheduleRepeating(delay, period.toLong(), generation, Runnable {
            if (!counter.acquire()) {
                return@Runnable
            }
            try {
                ActionRunner.runTriggeredLine(spec, flow, line, scope)
            } catch (exception: Exception) {
                warning("Action timer ${line.path} failed in skill '" + spec.skillId + "': " + exception.message)
            }
        })
        tasks.add(handle)
    }

    /**
     * 有限次数定时器的计数。
     *
     * @property limit 总次数，-1 表示不限。
     */
    private class RepeatCounter(private val limit: Int) {

        private var used = 0

        /**
         * 占用一次执行机会。
         *
         * @return 仍可执行时返回 true。
         */
        fun acquire(): Boolean {
            if (limit < 0) {
                return true
            }
            if (used >= limit) {
                return false
            }
            used += 1
            return true
        }
    }
}
