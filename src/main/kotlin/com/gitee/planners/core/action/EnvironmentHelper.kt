package com.gitee.planners.core.action

import com.gitee.planners.api.job.context.Context
import com.gitee.planners.api.job.target.TargetContainer
import taboolib.module.kether.ScriptFrame
import taboolib.module.kether.deepVars


fun ScriptFrame.getTargetContainer(): TargetContainer {
    return this.variables().get<TargetContainer>("@RUNNING_TEMP_CONTAINER").get();
}

fun ScriptFrame.getEnvironmentContext(): Context {
    return this.deepVars()["@RUNNING_ENVIRONMENT_CONTEXT"] as? Context ?: error("Error running environment !")
}

inline fun <reified T> Context.castUnsafely(): T? {
    return this as? T
}
