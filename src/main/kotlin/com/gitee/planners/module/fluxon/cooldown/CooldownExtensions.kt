package com.gitee.planners.module.fluxon.cooldown

import com.gitee.planners.api.Registries
import com.gitee.planners.api.job.Skill
import com.gitee.planners.core.skill.cooler.Cooler
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.getPlayerArg
import com.gitee.planners.module.fluxon.registerFunction
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * Cooldown 冷却系统扩展
 */
object CooldownExtensions {

    @Awake(LifeCycle.LOAD)
    fun init() {
        val runtime = FluxonScriptCache.runtime

        runtime.registerFunction("getCooldown", listOf(1, 2)) { ctx ->
            val skill = resolveSkill(ctx.getRef(0)) ?: return@registerFunction 0L
            val player = ctx.getPlayerArg(1) ?: return@registerFunction 0L
            Cooler.INSTANCE.get(player, skill)
        }

        runtime.registerFunction("setCooldown", listOf(2, 3)) { ctx ->
            val skill = resolveSkill(ctx.getRef(0)) ?: return@registerFunction null
            val ticks = ctx.getAsInt(1)
            val player = ctx.getPlayerArg(2) ?: return@registerFunction null
            Cooler.INSTANCE.set(player, skill, ticks)
            null
        }

        runtime.registerFunction("resetCooldown", listOf(1, 2)) { ctx ->
            val skill = resolveSkill(ctx.getRef(0)) ?: return@registerFunction null
            val player = ctx.getPlayerArg(1) ?: return@registerFunction null
            Cooler.INSTANCE.set(player, skill, 0)
            null
        }

        runtime.registerFunction("hasCooldown", listOf(1, 2)) { ctx ->
            val skill = resolveSkill(ctx.getRef(0)) ?: return@registerFunction false
            val player = ctx.getPlayerArg(1) ?: return@registerFunction false
            Cooler.INSTANCE.get(player, skill) > 0
        }
    }

    private fun resolveSkill(arg: Any?): Skill? {
        return when (arg) {
            is String -> Registries.SKILL.get(arg)
            is Skill -> arg
            else -> null
        }
    }
}
