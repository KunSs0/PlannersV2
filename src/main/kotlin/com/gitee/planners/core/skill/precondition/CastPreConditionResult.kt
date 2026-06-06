package com.gitee.planners.core.skill.precondition

/**
 * 释放前条件校验结果。
 */
sealed class CastPreConditionResult(val success: Boolean) {

    /** 校验通过 */
    object Success : CastPreConditionResult(true)

    /** 校验失败，携带失败条件和上下文数据 */
    class Failure(
        val condition: CastPreCondition,
        val context: Map<String, Any> = emptyMap()
    ) : CastPreConditionResult(false)

}
