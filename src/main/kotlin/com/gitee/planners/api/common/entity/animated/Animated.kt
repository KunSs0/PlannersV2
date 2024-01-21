package com.gitee.planners.api.common.entity.animated

import com.gitee.planners.api.common.Unique
import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.common.script.KetherScriptOptions
import com.gitee.planners.core.action.context.AbstractComplexScriptContext
import org.bukkit.event.Event
import taboolib.library.kether.Parser

interface Animated {

    fun listen(listener: AnimatedListener)

    fun emit(event: AnimatedEvent,context: AbstractComplexScriptContext)

    fun metaKeys(): Set<String>

    fun getMetadata(id: String): Metadata?


    interface Periodic {

        val timestampTick: Long

    }


}
