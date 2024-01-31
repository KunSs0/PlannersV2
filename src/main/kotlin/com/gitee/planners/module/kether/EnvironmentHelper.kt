package com.gitee.planners.module.kether

import com.gitee.planners.module.kether.context.Context
import com.gitee.planners.api.job.target.TargetContainer
import taboolib.module.kether.ScriptFrame
import taboolib.module.kether.deepVars


fun ScriptFrame.getTargetContainer(): TargetContainer {
    return this.variables().get<TargetContainer>("@RUNNING_TEMP_CONTAINER").get();
}

fun ScriptFrame.getEnvironmentContext(): Context {
    return this.deepVars()["@running-environment-context"] as? Context ?: error("Error running environment !")
}

inline fun <reified T> Context.castUnsafely(): T? {
    return this as? T
}
