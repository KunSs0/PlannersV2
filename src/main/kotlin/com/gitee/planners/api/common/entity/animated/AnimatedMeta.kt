package com.gitee.planners.api.common.entity.animated

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.common.metadata.MetadataTypeToken
import com.gitee.planners.core.action.*
import com.gitee.planners.util.unboxJavaToKotlin
import taboolib.library.kether.Parser
import taboolib.module.kether.ParserHolder

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
