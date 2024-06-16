package com.gitee.planners.core.skill

import com.sun.xml.internal.ws.util.CompletedFuture

enum class ExecutableResult() {

    COOLING,
    MAGICPOINT_INSUFFICIENT,
    SUCCESS;

    companion object {

        fun cooling() = ExecutableResult.COOLING

        fun magicPointInsufficient() = ExecutableResult.MAGICPOINT_INSUFFICIENT

        fun successful() = ExecutableResult.SUCCESS

    }

}
