package com.gitee.planners.api

import com.gitee.planners.api.common.DeepTrackRegistry
import com.gitee.planners.api.common.Registry
import com.gitee.planners.api.common.Unique
import com.gitee.planners.core.config.ImmutableJob
import com.gitee.planners.core.config.ImmutableRouter
import com.gitee.planners.core.config.ImmutableSkill
import taboolib.module.configuration.Configuration

object RegistryBuiltin {

    val JOB = deepTractRegistry("job") { ImmutableJob(this) }

    val SKILL = deepTractRegistry("skill") { ImmutableSkill(this) }

    val ROUTER = deepTractRegistry("router") { ImmutableRouter(this) }

    fun <T : Unique> deepTractRegistry(name: String, attachs : List<String> = emptyList(), block: Configuration.() -> T): Registry<T> {
        return DeepTrackRegistry<T>(name,attachs,block)
    }

}
