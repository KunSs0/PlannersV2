package com.gitee.planners.core.action

import com.gitee.planners.core.action.context.Context
import com.gitee.planners.api.job.target.TargetContainer
import taboolib.library.kether.LoadError
import taboolib.library.kether.ParsedAction
import taboolib.library.kether.QuestReader
import taboolib.module.kether.ScriptFrame
import taboolib.module.kether.deepVars
import taboolib.module.kether.literalAction


fun ScriptFrame.getTargetContainer(): TargetContainer {
    return this.variables().get<TargetContainer>("@RUNNING_TEMP_CONTAINER").get();
}

fun ScriptFrame.getEnvironmentContext(): Context {
    return this.deepVars()["@running-environment-context"] as? Context ?: error("Error running environment !")
}

inline fun <reified T> Context.castUnsafely(): T? {
    return this as? T
}
