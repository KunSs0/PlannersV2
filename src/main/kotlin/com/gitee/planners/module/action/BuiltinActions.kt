package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.Bukkit
import taboolib.common.platform.function.warning
import taboolib.platform.util.sendActionBar

/**
 * 基础动作实现。
 *
 * 只包含与宿主无关或仅依赖 Bukkit 通用能力的动作；战斗与外部插件能力
 * 分别由 `fc.*` / `mm.*` 文件维护。
 */
object BuiltinActions {

    /**
     * 聊天消息或 ActionBar。
     */
    object Message : Action {

        override val id = "message"

        override val aliases = listOf("message", "msg")

        override fun validate(arguments: ActionArguments) {
            arguments.require("m", "message")
            arguments.rejectUnknown("m", "message", "type", "t")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            val text = arguments.requireText(scope, "m", "message")
            val type = arguments.text(scope, "type", "t") ?: "chat"
            val actionBar = type.equals("actionbar", true)
            for (target in targets) {
                val player = target.bukkitPlayer()
                if (player == null) {
                    continue
                }
                if (actionBar) {
                    player.sendActionBar(text)
                } else {
                    player.sendMessage(text)
                }
            }
        }
    }

    /**
     * 执行命令，支持玩家身份与控制台身份。
     */
    object Command : Action {

        override val id = "command"

        override val aliases = listOf("command", "cmd")

        override fun validate(arguments: ActionArguments) {
            arguments.require("c", "command")
            arguments.rejectUnknown("c", "command", "as")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            val raw = arguments.requireText(scope, "c", "command")
            val command = raw.removePrefix("/")
            val sender = arguments.text(scope, "as") ?: "player"
            if (sender.equals("console", true)) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
                return
            }
            for (target in targets) {
                val player = target.bukkitPlayer()
                if (player == null) {
                    continue
                }
                player.performCommand(command)
            }
        }
    }

    /**
     * 插件日志。
     */
    object Log : Action {

        override val id = "log"

        override val aliases = listOf("log")

        override fun validate(arguments: ActionArguments) {
            arguments.require("text", "m", "message")
            arguments.rejectUnknown("text", "m", "message")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            val text = arguments.requireText(scope, "text", "m", "message")
            warning("[Planners] " + text)
        }
    }

    /**
     * 执行另一个流程。
     */
    object Call : Action {

        override val id = "call"

        override val aliases = listOf("call")

        override fun validate(arguments: ActionArguments) {
            arguments.require("func", "f")
            arguments.rejectUnknown("func", "f", "skill")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            // 流程跳转由运行时处理，本动作不直接执行副作用。
        }
    }

    /**
     * 延迟占位；实际调度由运行时读取参数完成。
     */
    object Delay : Action {

        override val id = "delay"

        override val aliases = listOf("delay", "wait")

        override fun validate(arguments: ActionArguments) {
            arguments.require("ticks", "t")
            arguments.rejectUnknown("ticks", "t")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            // 延迟由运行时处理，本动作不执行任何副作用。
        }
    }

    /**
     * 把当前目标容器写入命名缓存，同 id 覆盖。
     */
    object TargetFinderSet : Action {

        override val id = "target.finder.set"

        override val aliases = listOf("target.finder.set")

        override val acceptsEmptyTargets = true

        override fun validate(arguments: ActionArguments) {
            arguments.require("id")
            arguments.rejectUnknown("id")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            val key = arguments.requireText(scope, "id")
            TargetCache.store(scope, key, targets)
        }
    }
}
