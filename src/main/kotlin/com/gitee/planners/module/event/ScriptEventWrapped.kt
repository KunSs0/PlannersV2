package com.gitee.planners.module.event

import com.gitee.planners.api.common.entity.animated.Animated
import com.gitee.planners.api.job.target.Target
import taboolib.module.kether.ScriptContext

interface ScriptEventWrapped<T> {

    val name: String

    val bind: Class<T>

    fun getSender(event: T) : Target<*>?

    fun handle(event: T, ctx: ScriptContext)

    fun getModifier(event: T) : Animated? {
        return null
    }

}
