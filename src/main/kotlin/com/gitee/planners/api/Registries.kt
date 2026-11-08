package com.gitee.planners.api

import com.gitee.planners.Planners
import com.gitee.planners.core.config.*
import com.gitee.planners.core.condition.ConditionEvaluator
import com.gitee.planners.core.config.level.Algorithm
import com.gitee.planners.core.player.magic.DefaultMagicPointProvider
import com.gitee.planners.core.skill.SkillPointsManager
import com.gitee.planners.module.currency.OpenConvertibleCurrencyImpl
import com.gitee.planners.util.PhaseProfiler
import com.gitee.planners.util.builtin.AutoReloadable
import com.gitee.planners.util.builtin.createConfigSectionBuiltin
import com.gitee.planners.util.builtin.createDeepMultiBuiltin
import com.gitee.planners.util.builtin.createDeepSingleBuiltin
import taboolib.module.configuration.Configuration
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.info

object Registries {

    val JOB = createDeepSingleBuiltin(
        "job",
        "soldier/blade-master.yml",
        "soldier/grand-master.yml",
        "soldier/swordsman.yml"
    ) {
        ImmutableJob(it as Configuration)
    }

    // 只释放自包含教程与通用被动示例；本服业务技能一律放在数据目录。
    val SKILL = createDeepSingleBuiltin(
        "skill",
        "example0.yml",
        "example1.yml",
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
        Algorithm.Expression(it)
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
     * 重新读取业务配置，并以销毁旧实例、创建新实例的方式重建全部运行期索引。
     *
     * 此方法是 Planners 重载命令的上层编排入口。编译程序本身没有 reload 状态，
     * 任一新源码都只能在新程序加载前登记。
     */
    fun handleReload() {
        val profiler = PhaseProfiler("Reload profile")
        // 递增动作代际，取消上一代定时器与延迟任务，避免旧技能继续执行。
        profiler.measure("actions") {
            com.gitee.planners.module.action.ActionTimerService.stop()
            com.gitee.planners.module.action.ActionScheduler.nextGeneration()
        }
        // 先完整销毁旧资源树，确保旧调度任务与业务作用域不进入新配置代次。
        profiler.measure("config") { Planners.config.reload() }
        // 重载直接入口也必须清除旧施法续体，并严格校验新时间策略。
        profiler.measure("sessions") {
            com.gitee.planners.core.skill.timing.SkillCastingSessions.cancelAll()
        }
        profiler.measure("timing") { Planners.skillTimingPolicy.get() }
        // Registry 重读会重新构造业务对象。
        profiler.measure("registries") { AutoReloadable.onReload() }
        profiler.measure("points") { SkillPointsManager.prepare() }
        profiler.measure("magic") {
            DefaultMagicPointProvider.expressionUpperLimit.get()
            DefaultMagicPointProvider.expressionResume.get()
        }
        profiler.measure("actions") { rebuildActionIndex() }
        info(profiler.summary())
    }

    /**
     * 用当前已加载技能重建 Action 触发器索引、加载期流程与定时任务。
     *
     * 只收集写成结构化 `action` 的技能；旧脚本写法的技能不参与，
     * 因此迁移期间两种写法可以共存。
     */
    fun rebuildActionIndex() {
        val specs = ArrayList<com.gitee.planners.module.action.ActionSpec>()
        for (skill in SKILL.values()) {
            val spec = skill.actionSpec
            if (spec != null) {
                specs.add(spec)
            }
        }
        com.gitee.planners.module.action.ActionTriggerRegistry.rebuild(specs)
        com.gitee.planners.module.action.ActionServerFlows.runAll(specs)
        com.gitee.planners.module.action.ActionTimerService.restart(specs)
    }

    /**
     * 按业务依赖顺序加载所有注册表。
     */
    fun init() {
        val profiler = PhaseProfiler("Startup profile")
        // 按业务依赖顺序加载完整配置快照。
        profiler.measure("job") { JOB.load() }
        profiler.measure("skill") { SKILL.load() }
        profiler.measure("skilltree") { SKILL_TREE.load() }
        profiler.measure("router") { ROUTER.load() }
        profiler.measure("currency") { CURRENCY.load() }
        profiler.measure("level") { LEVEL.load() }
        profiler.measure("state") { STATE.load() }
        profiler.measure("keybinding") { KEYBINDING.load() }
        profiler.measure("actions") { rebuildActionIndex() }
        info(profiler.summary())
    }

}
