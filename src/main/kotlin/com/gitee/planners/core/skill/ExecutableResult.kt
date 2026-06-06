package com.gitee.planners.core.skill

import com.gitee.planners.core.skill.precondition.CastPreConditionResult

/**
 * 技能释放结果。
 */
sealed class ExecutableResult(val success: Boolean) {

    /** 释放成功 */
    object Success : ExecutableResult(true)

    /** 冷却中 */
    object Cooling : ExecutableResult(false)

    /** 魔法值不足（向后兼容） */
    object MagicPointInsufficient : ExecutableResult(false)

    /** 结束于事件 */
    object CancelledWithEvent : ExecutableResult(false)

    /** 被 SkillInputExecHook 接管 */
    class Intercepted(val cause: String) : ExecutableResult(false)

    /** 释放前条件校验失败 */
    class PreConditionFailed(val failure: CastPreConditionResult.Failure) : ExecutableResult(false)

    companion object {

        fun cooling() = Cooling

        fun magicPointInsufficient() = MagicPointInsufficient

        fun successful() = Success

        fun cancelledWithEvent() = CancelledWithEvent

        fun intercepted(cause: String) = Intercepted(cause)

        fun preConditionFailed(failure: CastPreConditionResult.Failure) = PreConditionFailed(failure)

    }

}
