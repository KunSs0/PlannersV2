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
     * 执行本结构内的另一个流程。
     *
     * 跨技能调用已拆到 `pl.skill.run` / `pl.skill.cast`，因此本动作只接受 `func`/`f`。
     */
    object Call : Action {

        override val id = "call"

        override val aliases = listOf("call")

        override fun validate(arguments: ActionArguments) {
            arguments.require("func", "f")
            arguments.rejectUnknown("func", "f")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            // 流程跳转由运行时处理，本动作不直接执行副作用。
        }
    }

    /**
     * 执行另一个技能的动作结构：API 级直调，不检查冷却、消耗与释放条件。
     *
     * `skill` 指定目标技能；`f` 可选，缺省执行目标技能的入口流程。
     * 施法者保持为当前上下文的施法者，写了目标器时按目标逐个展开。
     */
    object SkillRun : Action {

        override val id = "pl.skill.run"

        override val aliases = listOf(id)

        override fun validate(arguments: ActionArguments) {
            arguments.require("skill")
            arguments.rejectUnknown("skill", "func", "f")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            // 跨技能编排由运行时处理，本动作不直接执行副作用。
        }
    }

    /**
     * 释放另一个技能：走目标技能自己的冷却、消耗、释放条件与入口流程。
     *
     * `skill` 指定目标技能；施法者取当前上下文的施法者。
     */
    object SkillCast : Action {

        override val id = "pl.skill.cast"

        override val aliases = listOf(id)

        override fun validate(arguments: ActionArguments) {
            arguments.require("skill")
            arguments.rejectUnknown("skill")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            // 技能释放由运行时处理，本动作不直接执行副作用。
        }
    }

    /**
     * 条件分支：条件成立进入 `then` 流程，否则进入 `else` 流程。
     *
     * 条件在行执行时求值，分支通过运行时注入的流程入口同步跳转。
     */
    object IfBranch : Action {

        override val id = "if"

        override val aliases = listOf("if")

        override val acceptsEmptyTargets = true

        override fun validate(arguments: ActionArguments) {
            val raw = arguments.get("c", "cond", "condition")
            val conditionText = raw?.sourceText()
            if (conditionText == null) {
                throw IllegalArgumentException(arguments.path + " requires argument 'c'.")
            }
            ActionParser.parseCondition(conditionText, arguments.path + ".c")
            val thenFlow = arguments.get("then")?.sourceText()
            val elseFlow = arguments.get("else")?.sourceText()
            if ((thenFlow == null || thenFlow.isBlank()) && (elseFlow == null || elseFlow.isBlank())) {
                throw IllegalArgumentException(arguments.path + " requires then or else.")
            }
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            val raw = arguments.requireText(scope, "c", "cond", "condition")
            val condition = ActionParser.parseCondition(raw, arguments.path + ".c")
            val flowId = if (condition.matches(scope)) {
                arguments.text(scope, "then")
            } else {
                arguments.text(scope, "else")
            }
            if (flowId == null || flowId.isBlank()) {
                return
            }
            val runner = scope.flowRunner
            if (runner == null) {
                throw IllegalStateException("if 只能在动作结构执行链中使用。")
            }
            runner(flowId, scope)
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
            // 目标槽位是两个视图的同一份快照：@target.finder 读 TargetCache，
            // finder.<名字>.count 读 FinderCache。写一侧会让“名单正确但计数是上一跳”的错账无法排查。
            TargetCache.store(scope, key, targets)
            scope.finderCache[key] = ArrayList(targets)
        }
    }
}
