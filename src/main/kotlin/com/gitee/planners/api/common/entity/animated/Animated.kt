package com.gitee.planners.api.common.entity.animated

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.module.kether.context.AbstractComplexScriptContext

interface Animated {

    fun listen(listener: AnimatedListener)

    fun emit(event: AnimatedEvent, context: AbstractComplexScriptContext)

    fun metaKeys(): Set<String>

    fun getMetadata(id: String): Metadata?

    interface Periodic {

        val timestampTick: Long

    }

    interface Updated {

        fun handleUpdate(metadata: AnimatedMeta.CoerceMeta<Any>)

    }


}
