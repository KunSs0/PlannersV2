package com.gitee.planners.module.fluxon.common

import com.gitee.planners.api.Registries
import com.gitee.planners.api.job.Skill
import com.gitee.planners.core.skill.cooler.Cooler
import com.gitee.planners.module.fluxon.FluxonFunctionContext
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.registerFunction
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 通用扩展函数
 */
object CommonExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // tell(message, [sender]) -> void
        runtime.registerFunction("tell", listOf(1, 2)) { ctx ->
            val message = ctx.arguments[0]?.toString() ?: return@registerFunction null
            val sender = ctx.getSenderArg(1)
            sender.sendMessage(message)
            null
        }

        // setCooldown(skill, ticks, [player]) -> void
        runtime.registerFunction("setCooldown", listOf(2, 3)) { ctx ->
            val skill = ctx.resolveSkill(0) ?: return@registerFunction null
            val ticks = ctx.getAsInt(1)
            val player = ctx.getPlayerArg(2)
            if (player != null) {
                Cooler.INSTANCE.set(player, skill, ticks)
            }
            null
        }
    }

    private fun FluxonFunctionContext.getSenderArg(index: Int): CommandSender {
        if (arguments.size > index) {
            return arguments[index] as? CommandSender
                ?: throw IllegalStateException("Argument at $index is not a CommandSender")
        }
        return (environment.rootVariables["sender"] ?: environment.rootVariables["player"]) as? CommandSender
            ?: throw IllegalStateException("No sender found in environment")
    }

    private fun FluxonFunctionContext.getPlayerArg(index: Int): Player? {
        if (arguments.size > index) {
            return arguments[index] as? Player
        }
        return (environment.rootVariables["sender"] ?: environment.rootVariables["player"]) as? Player
    }

    private fun FluxonFunctionContext.resolveSkill(index: Int): Skill? {
        return when (val arg = arguments.getOrNull(index)) {
            is String -> Registries.SKILL.get(arg)
            is Skill -> arg
            else -> null
        }
    }
}
