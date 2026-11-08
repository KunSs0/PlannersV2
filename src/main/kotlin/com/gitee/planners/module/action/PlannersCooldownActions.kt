package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.Player

/**
 * `cooldown.arm`：开启一次运行时门禁。
 *
 * 配合 `off-cooldown` 条件使用：条件通过后立刻上锁，避免同一 tick 内重复触发。
 */
object PlannersCooldownActions {

    /**
     * 开锁动作。
     */
    object Arm : Action {

        override val id = "cooldown.arm"

        override val aliases = listOf("cooldown.arm")

        override val scope = ActionScope.SKILL

        override val acceptsEmptyTargets = true

        override fun validate(arguments: ActionArguments) {
            arguments.require("key")
            arguments.require("ticks")
            arguments.rejectUnknown("key", "ticks")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            val key = arguments.requireText(scope, "key")
            val ticks = arguments.requireInteger(scope, "ticks")
            val player = resolvePlayer(scope)
            if (player == null) {
                return
            }
            ActionCooldownGate.arm(player, key, ticks)
        }

        /**
         * 取门禁归属玩家。
         *
         * @param scope 执行上下文。
         * @return 玩家；取不到时返回 null。
         */
        private fun resolvePlayer(scope: QueryScope): Player? {
            val sender = scope.sender
            if (sender is Player) {
                return sender
            }
            val target = scope.target
            if (target is Player) {
                return target
            }
            return null
        }
    }
}
