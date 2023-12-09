package com.gitee.planners.api

import com.gitee.planners.api.common.DeepTrackRegistry
import com.gitee.planners.api.common.Registry
import com.gitee.planners.api.common.Unique
import com.gitee.planners.core.config.ImmutableJob
import com.gitee.planners.core.config.ImmutableRouter
import com.gitee.planners.core.config.ImmutableSkill
import taboolib.common.platform.Awake
import taboolib.module.configuration.Configuration

@Awake
object RegistryBuiltin {

    val JOB = deepTrackRegistry(
        "job",
        listOf("soldier/blade-master.yml", "soldier/grand-master.yml", "soldier/swordsman.yml")
    ) {
        ImmutableJob(this)
    }

    val SKILL = deepTrackRegistry("skill", listOf("example0.yml")) {
        ImmutableSkill(this)
    }

    val ROUTER = deepTrackRegistry("router", listOf("soldier.yml")) {
        ImmutableRouter(this)
    }

    fun <T : Unique> deepTrackRegistry(name: String, attaches: List<String> = emptyList(), block: Configuration.() -> T): Registry<T> {
        println("===== deep track registry")
        return object : DeepTrackRegistry<T>(name, attaches) {

            override fun invokeInstance(config: Configuration): T {
                return block(config)
            }

        }
    }

    fun handleReload() {
        DeepTrackRegistry.onReload()
    }

}
