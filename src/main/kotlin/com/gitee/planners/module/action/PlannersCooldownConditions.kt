package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.Player

/**
 * `off-cooldown`：运行时门禁是否已就绪。
 *
 * 与 `cooldown` 动作的区别是它不写技能冷却、不对外展示，只服务于
 * 「同一被动在 N tick 内不重复触发」这类内部节流。
 */
object PlannersCooldownConditions {

    /**
     * 门禁就绪判定。
     */
    object Ready : ActionCondition {

        override val id = "off-cooldown"

        override val aliases = listOf("off-cooldown")

        override val scope = ActionScope.SKILL

        override fun validate(arguments: ActionArguments) {
            arguments.require("key")
            arguments.rejectUnknown("key")
        }

        override fun test(scope: QueryScope, arguments: ActionArguments, compiled: Any?): Boolean {
            val key = arguments.requireText(scope, "key")
            val player = resolvePlayer(scope)
            if (player == null) {
                return false
            }
            return ActionCooldownGate.ready(player, key)
        }

        /**
         * 取判定玩家。
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
