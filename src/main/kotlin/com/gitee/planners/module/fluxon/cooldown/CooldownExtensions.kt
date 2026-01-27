package com.gitee.planners.module.fluxon.cooldown

import com.gitee.planners.api.Registries
import com.gitee.planners.api.job.Skill
import com.gitee.planners.core.skill.cooler.Cooler
import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type

/**
 * Cooldown 冷却系统扩展
 */
object CooldownExtensions {

    fun register() {
        val runtime = FluxonScriptCache.runtime

        // Player 冷却扩展
        runtime.registerExtension(Player::class.java)
            .function("getCooldown", FunctionSignature.returns(Type.J).params(Type.OBJECT)) { ctx ->
                val player = ctx.target ?: return@function
                val skillIdOrSkill = ctx.getRef(0)

                val skill = when (skillIdOrSkill) {
                    is String -> Registries.SKILL.get(skillIdOrSkill)
                    is Skill -> skillIdOrSkill
                    else -> return@function
                }

                ctx.setReturnLong(Cooler.INSTANCE.get(player, skill))
            }
            .function("setCooldown", FunctionSignature.returns(Type.VOID).params(Type.OBJECT, Type.I)) { ctx ->
                val player = ctx.target ?: return@function
                val skillIdOrSkill = ctx.getRef(0)
                val ticks = ctx.getAsInt(1)

                val skill = when (skillIdOrSkill) {
                    is String -> Registries.SKILL.get(skillIdOrSkill)
                    is Skill -> skillIdOrSkill
                    else -> return@function
                }

                Cooler.INSTANCE.set(player, skill, ticks)
            }
            .function("resetCooldown", FunctionSignature.returns(Type.VOID).params(Type.OBJECT)) { ctx ->
                val player = ctx.target ?: return@function
                val skillIdOrSkill = ctx.getRef(0)

                val skill = when (skillIdOrSkill) {
                    is String -> Registries.SKILL.get(skillIdOrSkill)
                    is Skill -> skillIdOrSkill
                    else -> return@function
                }

                Cooler.INSTANCE.set(player, skill, 0)
            }
            .function("hasCooldown", FunctionSignature.returns(Type.Z).params(Type.OBJECT)) { ctx ->
                val player = ctx.target ?: return@function
                val skillIdOrSkill = ctx.getRef(0)

                val skill = when (skillIdOrSkill) {
                    is String -> Registries.SKILL.get(skillIdOrSkill)
                    is Skill -> skillIdOrSkill
                    else -> return@function
                }

                ctx.setReturnBool(Cooler.INSTANCE.get(player, skill) > 0)
            }
    }
}
