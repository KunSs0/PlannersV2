package com.gitee.planners.api

import com.gitee.planners.api.common.registry.DeepTrackConfigurationRegistry
import com.gitee.planners.api.common.registry.ReloadableRegistry
import com.gitee.planners.api.common.registry.Unique
import com.gitee.planners.api.common.registry.deepTrackRegistry
import com.gitee.planners.core.config.ImmutableJob
import com.gitee.planners.core.config.ImmutableRouter
import com.gitee.planners.core.config.ImmutableSkill
import taboolib.common.platform.Awake
import taboolib.module.configuration.Configuration

@Awake
object RegistryBuiltin {

    val JOB = deepTrackRegistry("job", listOf("soldier/blade-master.yml", "soldier/grand-master.yml", "soldier/swordsman.yml")) {
        ImmutableJob(this)
    }

    val SKILL = deepTrackRegistry("skill", listOf("example0.yml")) {
        ImmutableSkill(this)
    }

    val ROUTER = deepTrackRegistry("router", listOf("soldier.yml")) {
        ImmutableRouter(this)
    }

    fun handleReload() {
        ReloadableRegistry.onReload()
    }

}
