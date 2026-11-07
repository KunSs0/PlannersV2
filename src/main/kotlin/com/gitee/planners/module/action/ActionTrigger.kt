package com.gitee.planners.module.action

/**
 * 触发器。
 *
 * 触发器只描述「这行什么时候执行」，不做条件判断——条件写在流程的
 * `conditions` 列表里。事件与定时两类触发器在流程执行到所在行时都会被跳过，
 * 分别等外部事件派发或定时器到期后才执行。
 */
interface ActionTrigger {

    /**
     * 触发器主键，小写；事件触发器以 `on` 开头。
     */
    val id: String

    /**
     * 允许的别名，包含主键。
     */
    val aliases: List<String>

    /**
     * 定时触发间隔。
     *
     * @param arguments 行上的触发器参数。
     * @return 间隔 tick；返回 null 表示由外部事件派发。
     */
    fun interval(arguments: ActionArguments): Int? {
        return null
    }

    /**
     * 定时器标识，声明后可通过卸载入口注销。
     *
     * @param arguments 行上的触发器参数。
     * @return 定时器 ID；未声明时返回 null。
     */
    fun identifier(arguments: ActionArguments): String? {
        return null
    }

    /**
     * 首次延迟 tick。
     *
     * @param arguments 行上的触发器参数。
     * @return 延迟 tick。
     */
    fun delay(arguments: ActionArguments): Int {
        return 0
    }

    /**
     * 触发次数，-1 表示不限。
     *
     * @param arguments 行上的触发器参数。
     * @return 次数。
     */
    fun times(arguments: ActionArguments): Int {
        return -1
    }

    /**
     * 加载期参数校验。
     *
     * @param arguments 行上的触发器参数。
     */
    fun validate(arguments: ActionArguments) {
    }
}
