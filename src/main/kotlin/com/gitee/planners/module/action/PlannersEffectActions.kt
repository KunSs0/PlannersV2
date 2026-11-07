package com.gitee.planners.module.action

import com.gitee.planners.api.effect.EffectProviders
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import taboolib.common.platform.function.warning

/**
 * 特效动作。
 *
 * 播放能力由外部插件通过 [EffectProviders] 注册，Planners 只负责解析参数与定位，
 * 未安装 Provider 时按未生效处理并记录，不影响后续动作。
 */
object PlannersEffectActions {

    /**
     * 在世界位置播放一次类型化特效。
     *
     * 参数 `at` 取 `self`（默认，施法者位置）或 `target`（当前目标位置）。
     */
    object Spawn : Action {

        override val id = "effect.spawn"

        override val aliases = listOf("effect.spawn")

        override val scope = ActionScope.SKILL

        override fun validate(arguments: ActionArguments) {
            arguments.require("id")
            arguments.require("ticks")
            arguments.rejectUnknown("id", "ticks", "at")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            val effectId = arguments.requireText(scope, "id")
            val ticks = arguments.requireInteger(scope, "ticks")
            if (ticks < 0) {
                throw IllegalArgumentException("${arguments.path} ticks cannot be negative.")
            }
            val at = arguments.text(scope, "at") ?: "self"
            val location = resolveLocation(scope, at, arguments.path)
            if (location == null) {
                return
            }
            val started = EffectProviders.spawnAtLocation(effectId, location, ticks)
            if (!started) {
                warning("Action ${arguments.path} failed to spawn effect '$effectId'.")
            }
        }

        /**
         * 解析播放位置。
         *
         * @param scope 查询上下文。
         * @param at `self` 或 `target`。
         * @param path 配置路径，用于错误信息。
         * @return 播放位置；无法解析时返回 null。
         */
        private fun resolveLocation(scope: QueryScope, at: String, path: String): Location? {
            if (at.equals("target", true)) {
                val target = scope.target
                if (target is LivingEntity) {
                    return target.location
                }
                return null
            }
            if (!at.equals("self", true)) {
                throw IllegalArgumentException("$path at must be self or target.")
            }
            val sender = scope.sender
            if (sender is LivingEntity) {
                return sender.location
            }
            if (sender is Location) {
                return sender
            }
            return null
        }
    }
}
