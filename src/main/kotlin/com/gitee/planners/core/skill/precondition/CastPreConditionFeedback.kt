package com.gitee.planners.core.skill.precondition

import org.bukkit.entity.Player

/**
 * 释放前条件校验失败时的反馈。
 *
 * 通过 [com.gitee.planners.api.PlannersAPI.setCastPreConditionFeedback] 替换默认实现。
 */
interface CastPreConditionFeedback {

    /**
     * 条件校验失败时的反馈（发消息、actionbar、sound 等）。
     */
    fun onFailed(player: Player, failure: CastPreConditionResult.Failure)

}
