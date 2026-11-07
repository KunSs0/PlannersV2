package com.gitee.planners.module.action

/**
 * 一条已编译的动作行。
 *
 * 动作在前，目标器按书写顺序在后，触发器最多一个挂在行尾。
 *
 * @property action 动作实现。
 * @property arguments 动作参数。
 * @property targeters 目标器链，按书写顺序。
 * @property trigger 行尾触发器；为空表示随流程立即执行。
 * @property triggerArguments 触发器参数；无触发器时为 null。
 * @property path 配置路径。
 * @property source 原始文本，用于诊断输出。
 */
class ActionLine(
    val action: Action,
    val arguments: ActionArguments,
    val targeters: List<ActionTargeterStep>,
    val trigger: ActionTrigger?,
    val triggerArguments: ActionArguments?,
    val path: String,
    val source: String,
) {

    /**
     * 是否带触发器。
     *
     * 带触发器的行在普通流程执行时会被跳过，等事件派发或定时器到期才执行。
     *
     * @return 带触发器时返回 true。
     */
    fun hasTrigger(): Boolean {
        return trigger != null
    }

    /**
     * 判断是否为定时触发器。
     *
     * @return 触发器声明了间隔时返回 true。
     */
    fun isTimer(): Boolean {
        val current = trigger
        val currentArguments = triggerArguments
        if (current == null || currentArguments == null) {
            return false
        }
        return current.interval(currentArguments) != null
    }

    /**
     * 判断是否匹配指定的触发器派发。
     *
     * 定时触发器不参与事件派发；派发参数必须与行上声明的参数一致。
     *
     * @param triggerId 触发器主键。
     * @return 匹配时返回 true。
     */
    fun matchesTrigger(triggerId: String): Boolean {
        val current = trigger
        if (current == null) {
            return false
        }
        if (current.id != triggerId) {
            return false
        }
        return !isTimer()
    }

    /**
     * 返回行的稳定描述，用于诊断输出。
     *
     * @return 形如 `damage{...} @sector{~on_hit}` 的文本。
     */
    fun describe(): String {
        val builder = StringBuilder()
        builder.append(action.id)
        if (arguments.keys.isNotEmpty()) {
            builder.append("{")
            var first = true
            for (key in arguments.keys) {
                if (!first) {
                    builder.append("; ")
                }
                builder.append(key)
                first = false
            }
            builder.append("}")
        }
        for (step in targeters) {
            builder.append(" ")
            builder.append(step.describe())
        }
        val current = trigger
        if (current != null) {
            builder.append(" ~")
            builder.append(current.id)
        }
        return builder.toString()
    }
}
