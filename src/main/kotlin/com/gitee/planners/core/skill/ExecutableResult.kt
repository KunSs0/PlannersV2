package com.gitee.planners.core.skill

import com.sun.xml.internal.ws.util.CompletedFuture

enum class ExecutableResult() {

    COOLING(),
    SUCCESS();

    companion object {

        fun cooling() = ExecutableResult.COOLING

        fun successful() = ExecutableResult.SUCCESS

    }

}
