package com.gitee.planners.api.common.entity.animated

import com.gitee.planners.module.fluxon.FluxonScriptOptions

interface AnimatedEvent {

    val name: String

    fun inject(options: FluxonScriptOptions)

}
