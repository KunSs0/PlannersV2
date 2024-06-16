package com.gitee.planners.api

import com.gitee.planners.api.common.registry.*
import com.gitee.planners.core.config.ImmutableJob
import com.gitee.planners.core.config.ImmutableRouter
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.module.currency.DefaultOpenConvertibleCurrency
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

    val CURRENCY = singletonRegistry("currency.yml") {
        DefaultOpenConvertibleCurrency(this)
    }

    fun handleReload() {
        ReloadableRegistry.onReload()
    }

}
