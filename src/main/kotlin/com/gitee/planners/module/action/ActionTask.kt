package com.gitee.planners.module.action

/**
 * 已安排的延迟或定时任务句柄。
 *
 * 由调度实现返回，用于在重载、停用或技能配置变更时主动取消。
 */
fun interface ActionTask {

    /**
     * 取消任务；已结束的任务调用无副作用。
     */
    fun cancel()
}
