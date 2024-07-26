package com.gitee.planners.api

import com.gitee.planners.core.config.ImmutableJob
import com.gitee.planners.core.config.ImmutableKeyBinding
import com.gitee.planners.core.config.ImmutableRouter
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.config.level.Algorithm
import com.gitee.planners.module.currency.DefaultOpenConvertibleCurrency
import com.gitee.planners.util.builtin.AutoReloadable
import com.gitee.planners.util.builtin.createDeepMultiBuiltin
import com.gitee.planners.util.builtin.createDeepSingleBuiltin
import com.gitee.planners.util.builtin.createSingleMultiBuiltin
import taboolib.common.platform.Awake
import taboolib.module.configuration.Configuration

object Registries {

    val JOB = createDeepSingleBuiltin(
        "job",
        "soldier/blade-master.yml",
        "soldier/grand-master.yml",
        "soldier/swordsman.yml"
    ) {
        ImmutableJob(it as Configuration)
    }

    val SKILL = createDeepSingleBuiltin("skill", "example0.yml") {
        ImmutableSkill(it as Configuration)
    }

    val ROUTER = createDeepSingleBuiltin("router", "soldier.yml") {
        ImmutableRouter(it as Configuration)
    }

    val CURRENCY = createDeepMultiBuiltin("module/currency", "example.yml") {
        DefaultOpenConvertibleCurrency(it)
    }

    val LEVEL = createDeepMultiBuiltin("module/level", "example.yml") {
        Algorithm.Kether(it)
    }

    val KEYBINDING = createSingleMultiBuiltin("key-binding.yml") {
        ImmutableKeyBinding(it)
    }

    fun handleReload() {
        AutoReloadable.onReload()
    }

    fun init() {
        // ...
    }

}
