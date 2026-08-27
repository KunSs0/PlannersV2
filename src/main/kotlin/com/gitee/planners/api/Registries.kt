package com.gitee.planners.api

import com.gitee.planners.Planners
import com.gitee.planners.core.config.*
import com.gitee.planners.core.config.level.Algorithm
import com.gitee.planners.core.player.magic.DefaultMagicPointProvider
import com.gitee.planners.core.skill.SkillPointsManager
import com.gitee.planners.module.currency.OpenConvertibleCurrencyImpl
import com.gitee.planners.module.script.ScriptManager
import com.gitee.planners.module.script.YamlNovaSourceCollector
import com.gitee.planners.util.builtin.AutoReloadable
import com.gitee.planners.util.builtin.createConfigSectionBuiltin
import com.gitee.planners.util.builtin.createDeepMultiBuiltin
import com.gitee.planners.util.builtin.createDeepSingleBuiltin
import taboolib.module.configuration.Configuration
import taboolib.common.platform.function.getDataFolder

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
        Algorithm.Nova(it)
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

    /**
     * 重新读取业务配置，并以销毁旧实例、创建新实例的方式重建 Nova Workspace。
     *
     * 此方法是 Planners 重载命令的上层编排入口。Workspace 本身没有 reload 状态，
     * 任一新源码都只能在新 Workspace 加载前登记。
     */
    fun handleReload() {
        // 先完整销毁旧资源树，确保旧调度任务与业务作用域不进入新配置代次。
        ScriptManager.dispose()
        Planners.config.reload()
        ScriptManager.prepare()
        YamlNovaSourceCollector.collect(getDataFolder().toPath())
        val configuredConditions = Planners.conditions.get()
        for (condition in configuredConditions.values) {
            condition.registerSources()
        }
        // Registry 重读会构造业务对象，并在 Workspace load 前登记其虚拟 SourceUnit。
        AutoReloadable.onReload()
        SkillPointsManager.prepareSources()
        DefaultMagicPointProvider.expressionUpperLimit.get()
        DefaultMagicPointProvider.expressionResume.get()
        ScriptManager.load()
    }

    /**
     * 按业务依赖顺序加载所有注册表，使构造阶段发现的 Nova 源码进入启动模块图。
     */
    fun init() {
        // 按业务依赖顺序加载完整配置快照，使全部 Nova 虚拟源在 Workspace load 前登记。
        JOB.load()
        SKILL.load()
        SKILL_TREE.load()
        ROUTER.load()
        CURRENCY.load()
        LEVEL.load()
        STATE.load()
        KEYBINDING.load()
    }

}
