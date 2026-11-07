package com.gitee.planners.module.action

/**
 * 一个流程：条件列表 + 有序动作行。
 *
 * @property id 流程名；`fire` 与 `server` 为保留名。
 * @property conditions 执行前必须全部成立的条件。
 * @property actions 按声明顺序执行的动作行。
 * @property path 配置路径。
 */
class ActionFlow(
    val id: String,
    val conditions: List<ActionConditionEntry>,
    val actions: List<ActionLine>,
    val path: String,
) {

    /**
     * 没有任何条件与动作时返回 true。
     *
     * @return 是否为空流程。
     */
    fun isEmpty(): Boolean {
        return conditions.isEmpty() && actions.isEmpty()
    }

    /**
     * 是否包含带触发器的行。
     *
     * @return 存在触发器行时返回 true。
     */
    fun hasTriggeredLines(): Boolean {
        for (line in actions) {
            if (line.hasTrigger()) {
                return true
            }
        }
        return false
    }
}
