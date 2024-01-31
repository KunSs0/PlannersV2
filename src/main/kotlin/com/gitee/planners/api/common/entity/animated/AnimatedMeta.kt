package com.gitee.planners.api.common.entity.animated

import com.gitee.planners.api.common.metadata.MetadataTypeToken

abstract class AnimatedMeta<T : Any>(
    val id: String,
    clazz: Class<*>,
    any: Any,
    val onUpdate: Animated.(data: T) -> Unit
) : MetadataTypeToken.TypeToken(clazz, any, -1) {

    fun set(data: T) {
        this.any = data
    }

    class CoerceMeta<T : Any>(
        id: String,
        clazz: Class<*>,
        any: Any,
        val parser: Any.() -> T,
        onUpdate: Animated.(data: T) -> Unit
    ) : AnimatedMeta<T>(
        id, clazz, any,
        onUpdate
    )

}
