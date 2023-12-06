package com.gitee.planners.api

import com.gitee.planners.api.common.Registry
import com.gitee.planners.api.common.Unique
import taboolib.module.configuration.Configuration

object RegistryBuiltin {

    val SKILL = configRegistry { "" }


    fun <T : Unique> configRegistry(block: Configuration.() -> T): Registry<T> {
        return object : Registry.AbstractBuiltin<T>() {

            override fun invokeInstance(config: Configuration): T = block(config)

        }
    }

}
