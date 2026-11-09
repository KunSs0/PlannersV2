package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.Player
import taboolib.common.platform.function.warning

/**
 * `timer.start`：在当前执行上下文里启动一个周期任务。
 *
 * 任务按 `every` tick 触发一次，每次以捕获的施法者与目标执行命名流程；
 * 句柄登记在当前作用域的 [TimerCache] 里，只能被同一执行内的 `timer.stop` 取消，
 * 不跨执行、不跨玩家。
 *
 * 同一 `id` 允许出现多个实例：同一段流程被执行两次就该启动两份，
 * 停止时一并取消该分组的全部实例。
 */
object TimerStartAction : Action {

    override val id = "timer.start"

    override val aliases = listOf("timer.start")

    override val acceptsEmptyTargets = true

    override fun validate(arguments: ActionArguments) {
        arguments.require("id")
        arguments.require("list")
        arguments.require("every")
        arguments.rejectUnknown("id", "list", "every", "times", "delay")
        val id = arguments.literalText("id")
        if (id != null && !TIMER_ID.matches(id.trim())) {
            throw IllegalArgumentException("${arguments.path} id 只允许 [A-Za-z0-9_.-]+。")
        }
        val every = arguments.literalInteger("every")
        if (every != null && every <= 0) {
            throw IllegalArgumentException("${arguments.path} every must be positive.")
        }
        val times = arguments.literalInteger("times")
        if (times != null && times <= 0) {
            throw IllegalArgumentException("${arguments.path} times must be positive.")
        }
        val delay = arguments.literalInteger("delay")
        if (delay != null && delay < 0) {
            throw IllegalArgumentException("${arguments.path} delay cannot be negative.")
        }
    }

    override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
        val id = arguments.requireText(scope, "id")
        if (!TIMER_ID.matches(id)) {
            throw IllegalArgumentException("${arguments.path} id 只允许 [A-Za-z0-9_.-]+。")
        }
        val flow = arguments.requireText(scope, "list")
        val every = arguments.requireInteger(scope, "every")
        if (every <= 0) {
            throw IllegalArgumentException("${arguments.path} every must be positive.")
        }
        val rawTimes = arguments.integer(scope, "times")
        if (rawTimes != null && rawTimes <= 0) {
            throw IllegalArgumentException("${arguments.path} times must be positive.")
        }
        val delay = arguments.integer(scope, "delay") ?: 0
        if (delay < 0) {
            throw IllegalArgumentException("${arguments.path} delay cannot be negative.")
        }
        val runner = scope.flowRunner
        if (runner == null) {
            throw IllegalStateException("${arguments.path} 只能在技能流程里启动定时任务。")
        }
        val captured = scope
        val generation = ActionScheduler.currentGeneration()
        var remaining = rawTimes ?: UNLIMITED
        var handle: ActionTask? = null
        handle = ActionScheduler.scheduleRepeating(delay.toLong(), every.toLong(), generation) {
            val task = handle
            if (task == null) {
                return@scheduleRepeating
            }
            if (!isSenderAlive(captured.sender)) {
                task.cancel()
                return@scheduleRepeating
            }
            if (remaining == 0) {
                task.cancel()
                return@scheduleRepeating
            }
            try {
                runner.invoke(flow, TimerScopes.fresh(captured))
            } catch (exception: Exception) {
                warning("Action timer ${arguments.path} failed in '$flow': " + exception.message)
            }
            if (remaining > 0) {
                remaining -= 1
            }
            if (remaining == 0) {
                task.cancel()
            }
        }
        scope.timerCache.store(id, handle)
    }

    /**
     * 判断捕获的施法者是否仍然可用。
     *
     * @param sender 捕获的施法者。
     * @return 非玩家或玩家仍在线时返回 true。
     */
    private fun isSenderAlive(sender: Any?): Boolean {
        if (sender !is Player) {
            return true
        }
        return sender.isOnline && sender.isValid && !sender.isDead
    }

    /** 不限次数。 */
    private const val UNLIMITED = -1

    /** 任务分组名允许的字符。 */
    private val TIMER_ID = Regex("^[A-Za-z0-9_.-]+$")
}
