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
     * 判断本次派发是否命中该行声明的触发器参数。
     *
     * 默认全部命中：只按主键匹配派发的触发器无需覆盖。需要按载荷筛选的
     * 事件触发器（例如弹道命中按 PlayerBullet ID 过滤）覆盖本方法。
     *
     * @param scope 本次派发的执行上下文，载荷在 [com.gitee.planners.module.action.query.QueryScope.payload]。
     * @param arguments 行上声明的触发器参数。
     * @return 命中时返回 true，该行才会执行。
     */
    fun matches(scope: com.gitee.planners.module.action.query.QueryScope, arguments: ActionArguments): Boolean {
        return true
    }

    /**
     * 该触发器派发时能提供的事件实体选择器。
     *
     * 事件实体放在执行载荷里，对应关系由触发器自己声明：例如伤害类触发器
     * 同时提供 `attacker` 与 `defender`，Buff 类只提供 `target`。
     * 加载期据此校验 `@attacker` 这类选择器是否用错触发器。
     *
     * @return 可用的选择器键名；定时器等无事件触发器返回空集合。
     */
    val providedTargets: Set<String>
        get() {
            return emptySet()
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
