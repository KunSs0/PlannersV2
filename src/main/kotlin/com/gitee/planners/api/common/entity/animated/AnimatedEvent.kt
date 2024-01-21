package com.gitee.planners.api.common.entity.animated

import taboolib.module.kether.ScriptContext

interface AnimatedEvent {

    val name: String

    fun inject(ctx: ScriptContext)

}
