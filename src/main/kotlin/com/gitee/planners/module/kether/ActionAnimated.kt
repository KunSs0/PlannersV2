package com.gitee.planners.module.kether

import com.gitee.planners.api.common.entity.animated.Animated
import com.gitee.planners.api.common.entity.animated.AnimatedMeta
import taboolib.common.OpenResult
import taboolib.module.kether.*


object ActionAnimated {

    @KetherProperty(bind = Animated::class)
    fun propertyAnimated() = object : ScriptProperty<Animated>("animated.operator") {

        override fun read(instance: Animated, key: String): OpenResult {
            val metadata = instance.getMetadata(key) as AnimatedMeta<*>
            return OpenResult.successful(metadata.any())
        }

        override fun write(instance: Animated, key: String, value: Any?): OpenResult {
            val metadata = instance.getMetadata(key) as AnimatedMeta.CoerceMeta<Any>
            metadata.set(metadata.parser(value!!))
            if (instance is Animated.Updated) {
                instance.handleUpdate(metadata)
            }
            return OpenResult.successful()
        }

    }

}






