package com.gitee.planners.module.fluxon.cooldown

import com.gitee.planners.api.Registries
import com.gitee.planners.api.job.Skill
import com.gitee.planners.core.skill.cooler.Cooler
import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionContext
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * Cooldown 冷却系统扩展 - 全局函数注册
 */
object CooldownExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // getCooldown(skillIdOrSkill) -> long (从环境获取player)
        runtime.registerFunction("getCooldown", FunctionSignature.returns(Type.J).params(Type.OBJECT)) { ctx ->
            val skillIdOrSkill = ctx.getRef(0)
            val player = getPlayerFromEnv(ctx)
            val skill = resolveSkill(skillIdOrSkill) ?: return@registerFunction
            ctx.setReturnLong(Cooler.INSTANCE.get(player, skill))
        }

        // getCooldown(skillIdOrSkill, player) -> long
        runtime.registerFunction("getCooldown", FunctionSignature.returns(Type.J).params(Type.OBJECT, Type.OBJECT)) { ctx ->
            val skillIdOrSkill = ctx.getRef(0)
            val player = ctx.getRef(1) as? Player ?: return@registerFunction
            val skill = resolveSkill(skillIdOrSkill) ?: return@registerFunction
            ctx.setReturnLong(Cooler.INSTANCE.get(player, skill))
        }

        // setCooldown(skillIdOrSkill, ticks) -> void (从环境获取player)
        runtime.registerFunction("setCooldown", FunctionSignature.returns(Type.VOID).params(Type.OBJECT, Type.I)) { ctx ->
            val skillIdOrSkill = ctx.getRef(0)
            val ticks = ctx.getAsInt(1)
            val player = getPlayerFromEnv(ctx)
            val skill = resolveSkill(skillIdOrSkill) ?: return@registerFunction
            Cooler.INSTANCE.set(player, skill, ticks)
        }

        // setCooldown(skillIdOrSkill, ticks, player) -> void
        runtime.registerFunction("setCooldown", FunctionSignature.returns(Type.VOID).params(Type.OBJECT, Type.I, Type.OBJECT)) { ctx ->
            val skillIdOrSkill = ctx.getRef(0)
            val ticks = ctx.getAsInt(1)
            val player = ctx.getRef(2) as? Player ?: return@registerFunction
            val skill = resolveSkill(skillIdOrSkill) ?: return@registerFunction
            Cooler.INSTANCE.set(player, skill, ticks)
        }

        // resetCooldown(skillIdOrSkill) -> void (从环境获取player)
        runtime.registerFunction("resetCooldown", FunctionSignature.returns(Type.VOID).params(Type.OBJECT)) { ctx ->
            val skillIdOrSkill = ctx.getRef(0)
            val player = getPlayerFromEnv(ctx)
            val skill = resolveSkill(skillIdOrSkill) ?: return@registerFunction
            Cooler.INSTANCE.set(player, skill, 0)
        }

        // resetCooldown(skillIdOrSkill, player) -> void
        runtime.registerFunction("resetCooldown", FunctionSignature.returns(Type.VOID).params(Type.OBJECT, Type.OBJECT)) { ctx ->
            val skillIdOrSkill = ctx.getRef(0)
            val player = ctx.getRef(1) as? Player ?: return@registerFunction
            val skill = resolveSkill(skillIdOrSkill) ?: return@registerFunction
            Cooler.INSTANCE.set(player, skill, 0)
        }

        // hasCooldown(skillIdOrSkill) -> boolean (从环境获取player)
        runtime.registerFunction("hasCooldown", FunctionSignature.returns(Type.Z).params(Type.OBJECT)) { ctx ->
            val skillIdOrSkill = ctx.getRef(0)
            val player = getPlayerFromEnv(ctx)
            val skill = resolveSkill(skillIdOrSkill) ?: run {
                ctx.setReturnBool(false)
                return@registerFunction
            }
            ctx.setReturnBool(Cooler.INSTANCE.get(player, skill) > 0)
        }

        // hasCooldown(skillIdOrSkill, player) -> boolean
        runtime.registerFunction("hasCooldown", FunctionSignature.returns(Type.Z).params(Type.OBJECT, Type.OBJECT)) { ctx ->
            val skillIdOrSkill = ctx.getRef(0)
            val player = ctx.getRef(1) as? Player ?: run {
                ctx.setReturnBool(false)
                return@registerFunction
            }
            val skill = resolveSkill(skillIdOrSkill) ?: run {
                ctx.setReturnBool(false)
                return@registerFunction
            }
            ctx.setReturnBool(Cooler.INSTANCE.get(player, skill) > 0)
        }
    }

    private fun resolveSkill(skillIdOrSkill: Any?): Skill? {
        return when (skillIdOrSkill) {
            is String -> Registries.SKILL.get(skillIdOrSkill)
            is Skill -> skillIdOrSkill
            else -> null
        }
    }

    private fun getPlayerFromEnv(ctx: FunctionContext<*>): Player {
        val find = ctx.environment.rootVariables["player"]
        if (find is Player) {
            return find
        }
        throw IllegalStateException("No player found in environment")
    }
}
