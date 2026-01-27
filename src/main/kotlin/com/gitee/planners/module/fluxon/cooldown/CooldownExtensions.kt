package com.gitee.planners.module.fluxon.cooldown

import com.gitee.planners.api.Registries
import com.gitee.planners.api.job.Skill
import com.gitee.planners.core.skill.cooler.Cooler
import com.gitee.planners.module.fluxon.FluxonFunctionContext
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.registerFunction
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * Cooldown 冷却系统扩展
 */
object CooldownExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // getCooldown(skillIdOrSkill, [player]) -> long
        runtime.registerFunction("getCooldown", listOf(1, 2)) { ctx ->
            val skill = resolveSkill(ctx.arguments[0]) ?: return@registerFunction 0L
            val player = ctx.getPlayerArg(1)
            Cooler.INSTANCE.get(player, skill)
        }

        // setCooldown(skillIdOrSkill, ticks, [player]) -> void
        runtime.registerFunction("setCooldown", listOf(2, 3)) { ctx ->
            val skill = resolveSkill(ctx.arguments[0]) ?: return@registerFunction null
            val ticks = (ctx.arguments[1] as Number).toInt()
            val player = ctx.getPlayerArg(2)
            Cooler.INSTANCE.set(player, skill, ticks)
            null
        }

        // resetCooldown(skillIdOrSkill, [player]) -> void
        runtime.registerFunction("resetCooldown", listOf(1, 2)) { ctx ->
            val skill = resolveSkill(ctx.arguments[0]) ?: return@registerFunction null
            val player = ctx.getPlayerArg(1)
            Cooler.INSTANCE.set(player, skill, 0)
            null
        }

        // hasCooldown(skillIdOrSkill, [player]) -> boolean
        runtime.registerFunction("hasCooldown", listOf(1, 2)) { ctx ->
            val skill = resolveSkill(ctx.arguments[0]) ?: return@registerFunction false
            val player = ctx.getPlayerArg(1)
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

    private fun FluxonFunctionContext.getPlayerArg(index: Int): Player {
        if (arguments.size > index) {
            return arguments[index] as? Player
                ?: throw IllegalStateException("Argument at $index is not a player")
        }
        return environment.rootVariables["player"] as? Player
            ?: throw IllegalStateException("No player found in environment")
    }
}
