package com.gitee.planners.core.skill.precondition

import org.bukkit.entity.Player

/**
 * 默认反馈实现：通过 chat 发送提示消息。
 */
class DefaultCastPreConditionFeedback : CastPreConditionFeedback {

    override fun onFailed(player: Player, failure: CastPreConditionResult.Failure) {
        var message = failure.condition.hint(player, failure)
        failure.context.forEach { (key, value) ->
            message = message.replace("{$key}", value.toString())
        }
        player.sendMessage("§c$message")
    }

}
