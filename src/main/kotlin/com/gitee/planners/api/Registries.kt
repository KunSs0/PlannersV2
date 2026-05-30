package com.gitee.planners.api

import com.gitee.planners.Planners
import com.gitee.planners.api.damage.DamageCause
import com.gitee.planners.core.config.*
import com.gitee.planners.core.config.level.Algorithm
import com.gitee.planners.module.currency.OpenConvertibleCurrencyImpl
import com.gitee.planners.util.builtin.AutoReloadable
import com.gitee.planners.util.builtin.createConfigSectionBuiltin
import com.gitee.planners.util.builtin.createDeepMultiBuiltin
import com.gitee.planners.util.builtin.createDeepSingleBuiltin
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

    val SKILL_TREE = createDeepMultiBuiltin("skilltree", "warrior.yml") {
        ImmutableSkillTree.parse(it.name, it)
    }

    val ROUTER = createDeepSingleBuiltin("router", "soldier.yml") {
        ImmutableRouter(it as Configuration)
    }

    val CURRENCY = createDeepMultiBuiltin("module/currency", "example.yml") {
        OpenConvertibleCurrencyImpl(it)
    }

    val LEVEL = createDeepMultiBuiltin("module/level", "example.yml") {
        Algorithm.Js(it)
    }

    val KEYBINDING = createConfigSectionBuiltin({
        Planners.config.getConfigurationSection("settings.keybinding.keymapping")
    }) {
        ImmutableKeyBinding(it)
    }

    val STATE = createDeepMultiBuiltin("state", "example.yml") {
        ImmutableState(it)
    }

    val BACKPACK: BackpackConfig
        get() = Planners.backpackConfig.get()

    fun handleReload() {
        DamageCause.reload()
        AutoReloadable.onReload()
    }

    fun init() {
        DamageCause.reload()
    }

}
