package com.gitee.planners.api.common.entity.animated

import com.gitee.planners.api.common.metadata.MetadataTypeToken

abstract class AnimatedMeta<T : Any>(
    val stack: Animated,
    val id: String,
    clazz: Class<*>,
    any: Any,
    val onUpdate: Animated.(data: T) -> Unit
) : MetadataTypeToken.TypeToken(clazz, any, -1) {

    fun set(data: T) {
        this.any = data
    }

    fun setAsUpdate(data: T) {
        this.set(data)
        this.onUpdate(stack, data)
    }

    class CoerceMeta<T : Any>(
        stack: Animated,
        id: String,
        clazz: Class<*>,
        any: Any,
        val parser: Any.() -> T,
        onUpdate: Animated.(data: T) -> Unit
    ) : AnimatedMeta<T>(stack, id, clazz, any, onUpdate)

}
