package com.gitee.planners.api.common.entity.animated

import com.gitee.planners.api.common.metadata.Metadata

interface Animated {

    fun listen(listener: AnimatedListener)

    fun emit(event: AnimatedEvent, sender: Any, variables: Map<String, Any?> = emptyMap())

    fun metaKeys(): Set<String>

    fun getMetadata(id: String): Metadata?

    interface Periodic {

        val timestampTick: Long

    }

    interface Updated {

        fun handleUpdate(metadata: AnimatedMeta.CoerceMeta<Any>)

    }


}
