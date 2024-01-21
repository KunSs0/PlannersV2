package com.gitee.planners.core.action

import com.gitee.planners.api.common.entity.animated.Animated
import com.gitee.planners.api.common.entity.animated.AnimatedMeta
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.SimpleKetherParser
import com.mojang.datafixers.kinds.App
import taboolib.common.OpenResult
import taboolib.common.util.orNull
import taboolib.library.kether.ParsedAction
import taboolib.library.kether.Parser
import taboolib.library.kether.QuestActionParser
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
            return OpenResult.successful()
        }

    }

}






