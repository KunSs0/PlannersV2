package com.gitee.planners.core.skill


enum class ExecutableResult() {

    /* 冷却中 */
    COOLING,

    /* 魔法点不足 */
    MAGICPOINT_INSUFFICIENT,

    /* 结束于事件 */
    CANCEL_WITH_EVENT,

    /* 被 SkillInputExecHook 接管 */
    INTERCEPTED,

    /* 成功 */
    SUCCESS;

    companion object {

        /** 最近一次 INTERCEPTED 的原因 */
        var lastInterceptCause: String = ""
            private set

        fun cooling() = ExecutableResult.COOLING

        fun magicPointInsufficient() = ExecutableResult.MAGICPOINT_INSUFFICIENT

        fun successful() = ExecutableResult.SUCCESS

        fun cancelledWithEvent() = ExecutableResult.CANCEL_WITH_EVENT

        fun intercepted(cause: String): ExecutableResult {
            lastInterceptCause = cause
            return ExecutableResult.INTERCEPTED
        }

    }

}
