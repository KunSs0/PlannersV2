package com.gitee.planners.core.skill

import com.sun.xml.internal.ws.util.CompletedFuture

enum class ExecutableResult() {

    /* 冷却中 */
    COOLING,

    /* 魔法点不足 */
    MAGICPOINT_INSUFFICIENT,

    /* 结束于事件 */
    CANCEL_WITH_EVENT,

    /* 成功 */
    SUCCESS;

    companion object {

        fun cooling() = ExecutableResult.COOLING

        fun magicPointInsufficient() = ExecutableResult.MAGICPOINT_INSUFFICIENT

        fun successful() = ExecutableResult.SUCCESS

        fun cancelledWithEvent() = ExecutableResult.CANCEL_WITH_EVENT

    }

}
