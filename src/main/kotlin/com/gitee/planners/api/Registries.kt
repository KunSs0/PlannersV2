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

    val SKILL = createDeepSingleBuiltin(
        "skill",
        "example0.yml",
        "example1.yml",
        "slash.yml",
        "charge.yml",
        "shield_bash.yml",
        "counter_strike.yml",
        "heavy_slash.yml",
        "war_cry.yml",
        "iron_will.yml",
        "thunder_clap.yml",
        "whirlwind.yml",
        "battle_fury.yml",
        "earth_splitter.yml",
        "berserk.yml",
        "blade_storm.yml",
        "blood_lust.yml",
        "last_stand.yml",
        "passive_toughness.yml",
        "passive_rage.yml"
    ) {
        ImmutableSkill(it as Configuration)
    }

    val SKILL_TREE = createDeepMultiBuiltin("skilltree", "example.yml", "warrior_vanguard.yml") {
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
