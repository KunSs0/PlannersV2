package com.gitee.planners.module.action

/**
 * 一次执行内启动的定时任务句柄。
 *
 * 句柄只存在当前作用域，不跨执行、不跨玩家、不跨重载；同一 id 可以对应多个实例，
 * 因为同一段流程可能被执行两次，两次都应当各自启动一份。
 */
class TimerCache {

    private val entries = LinkedHashMap<String, MutableList<ActionTask>>()

    /**
     * 登记一个任务句柄，同 id 追加而不是覆盖。
     *
     * @param id 任务分组名。
     * @param task 任务句柄。
     */
    fun store(id: String, task: ActionTask) {
        val list = entries[id]
        if (list == null) {
            entries[id] = mutableListOf(task)
            return
        }
        list.add(task)
    }

    /**
     * 取消并移除指定分组的全部任务。
     *
     * @param id 任务分组名。
     * @return 取消的任务数量；没有该分组时返回 0。
     */
    fun cancel(id: String): Int {
        val list = entries.remove(id)
        if (list == null) {
            return 0
        }
        for (task in list) {
            task.cancel()
        }
        return list.size
    }

    /**
     * 取消全部任务。
     */
    fun cancelAll() {
        for (list in entries.values) {
            for (task in list) {
                task.cancel()
            }
        }
        entries.clear()
    }

    /**
     * 判断是否登记过指定分组。
     *
     * @param id 任务分组名。
     * @return 存在时返回 true。
     */
    fun contains(id: String): Boolean {
        return entries.containsKey(id)
    }
}
