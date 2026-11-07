package com.gitee.planners.module.action

/**
 * 内置触发器实现。
 *
 * 只提供定时触发器；事件触发器按事件来源分别在 `fc.on_*` 与 `on_*` 文件中维护。
 */
object BuiltinTriggers {

    /**
     * 定时触发器。
     *
     * 支持 `~onTimer{ticks=20}` 与 `~onTimer:20` 两种写法；
     * 周期为字面量时在加载期固化，为 `<>` 查询时由运行时每次到期重新求值。
     */
    object Timer : ActionTrigger {

        override val id = "onTimer"

        override val aliases = listOf("ontimer", "timer", "t")

        override fun validate(arguments: ActionArguments) {
            arguments.rejectUnknown("ticks", "t", "interval", "id", "delay", "d", "times", "n")
            if (!arguments.contains("ticks") && !arguments.contains("t") && !arguments.contains("interval")) {
                throw IllegalArgumentException("${arguments.path} requires an integer 'ticks'.")
            }
            val ticks = arguments.literalInteger("ticks", "t", "interval")
            if (ticks == null) {
                throw IllegalArgumentException(
                    "${arguments.path} ticks must be a literal integer: 定时器周期在加载期注册，不能依赖运行期取值。",
                )
            }
            if (ticks <= 0) {
                throw IllegalArgumentException("${arguments.path} ticks must be positive.")
            }
            val delay = arguments.literalInteger("delay", "d")
            if (delay != null && delay < 0) {
                throw IllegalArgumentException("${arguments.path} delay cannot be negative.")
            }
            val times = arguments.literalInteger("times", "n")
            if (times != null && times == 0) {
                throw IllegalArgumentException("${arguments.path} times cannot be zero.")
            }
        }

        override fun interval(arguments: ActionArguments): Int? {
            return arguments.literalInteger("ticks", "t", "interval")
        }

        override fun identifier(arguments: ActionArguments): String? {
            val value = arguments.literalText("id")
            if (value == null) {
                return null
            }
            val trimmed = value.trim()
            if (trimmed.isEmpty()) {
                throw IllegalArgumentException("${arguments.path} timer id cannot be blank.")
            }
            if (!TIMER_ID.matches(trimmed)) {
                throw IllegalArgumentException("${arguments.path} timer id must match [A-Za-z0-9_.-]+.")
            }
            return trimmed
        }

        override fun delay(arguments: ActionArguments): Int {
            val value = arguments.literalInteger("delay", "d")
            if (value == null) {
                return 0
            }
            return value
        }

        override fun times(arguments: ActionArguments): Int {
            val value = arguments.literalInteger("times", "n")
            if (value == null) {
                return -1
            }
            return value
        }

        /** 定时器 ID 允许的字符。 */
        private val TIMER_ID = Regex("[A-Za-z0-9_.-]+")
    }
}
