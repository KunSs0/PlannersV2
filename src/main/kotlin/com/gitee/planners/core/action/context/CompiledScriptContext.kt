package com.gitee.planners.core.action.context

import com.gitee.planners.api.common.script.ComplexCompiledScript
import com.gitee.planners.api.job.target.Target
import taboolib.library.kether.Quest

class CompiledScriptContext(sender: Target<*>, compiled: ComplexCompiledScript) : AbstractComplexScriptContext(sender, compiled) {
    override val trackId: String
        get() = compiled.id


}
