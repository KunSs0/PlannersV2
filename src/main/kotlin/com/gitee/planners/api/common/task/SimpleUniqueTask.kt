package com.gitee.planners.api.common.task

class SimpleUniqueTask(val id: Any, tick: Long, async: Boolean, onClose: Runnable) :
    SimpleFutureTask(tick, async, onClose) {

    var isDone = false
        private set

    override fun handleClose() {
        // 是否成功中断
        if (this.interrupt()) {
            super.handleClose()
        }
    }

    fun close() {
        this.handleClose()
    }

    fun interrupt() : Boolean {
        if (!isDone) {
            this.isDone = true
            return true
        }
        return false
    }

    companion object {

        private val registeredTask = mutableMapOf<Any, SimpleUniqueTask>()

        fun create(id: Any, tick: Long, async: Boolean, onClose: Runnable) {
            val task = registeredTask[id]
            // 如果任务存在 并且任务没有完成 则中断上次运行的任务
            if (task != null && !task.isDone) {
                task.interrupt()
            }
            registeredTask[id] = SimpleUniqueTask(id, tick, async, onClose)
        }

        fun getTask(id: Any): SimpleUniqueTask? {
            return registeredTask[id]
        }

    }

}
