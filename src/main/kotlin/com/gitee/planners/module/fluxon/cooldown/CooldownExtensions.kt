package com.gitee.planners.module.fluxon.cooldown

import com.gitee.planners.core.skill.cooler.Cooler
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.getPlayerArg
import com.gitee.planners.module.fluxon.resolveSkill
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * Cooldown 冷却系统扩展
 */
object CooldownExtensions {

    @Awake(LifeCycle.LOAD)
    fun init() {
        val runtime = FluxonScriptCache.runtime

        // getCooldown(skill) - 获取 sender 的技能冷却
        runtime.registerFunction("getCooldown", returns(Type.NUMBER).params(Type.OBJECT)) { ctx ->
            val skill = resolveSkill(ctx.getRef(0)) ?: return@registerFunction
            val player = ctx.getPlayerArg(-1) ?: return@registerFunction
            ctx.setReturnLong(Cooler.INSTANCE.get(player, skill))
        }

        // getCooldown(skill, player) - 获取指定玩家的技能冷却
        runtime.registerFunction("getCooldown", returns(Type.NUMBER).params(Type.OBJECT, Type.OBJECT)) { ctx ->
            val skill = resolveSkill(ctx.getRef(0)) ?: return@registerFunction
            val player = ctx.getPlayerArg(1) ?: return@registerFunction
            ctx.setReturnLong(Cooler.INSTANCE.get(player, skill))
        }

        // setCooldown(skill, ticks) - 设置 sender 的技能冷却
        runtime.registerFunction("setCooldown", returns(Type.VOID).params(Type.OBJECT, Type.NUMBER)) { ctx ->
            val skill = resolveSkill(ctx.getRef(0)) ?: return@registerFunction
            val ticks = ctx.getAsInt(1)
            val player = ctx.getPlayerArg(-1) ?: return@registerFunction
            Cooler.INSTANCE.set(player, skill, ticks)
        }

        // setCooldown(skill, ticks, player) - 设置指定玩家的技能冷却
        runtime.registerFunction("setCooldown", returns(Type.VOID).params(Type.OBJECT, Type.NUMBER, Type.OBJECT)) { ctx ->
            val skill = resolveSkill(ctx.getRef(0)) ?: return@registerFunction
            val ticks = ctx.getAsInt(1)
            val player = ctx.getPlayerArg(2) ?: return@registerFunction
            Cooler.INSTANCE.set(player, skill, ticks)
        }

        // resetCooldown(skill) - 重置 sender 的技能冷却
        runtime.registerFunction("resetCooldown", returns(Type.VOID).params(Type.OBJECT)) { ctx ->
            val skill = resolveSkill(ctx.getRef(0)) ?: return@registerFunction
            val player = ctx.getPlayerArg(-1) ?: return@registerFunction
            Cooler.INSTANCE.set(player, skill, 0)
        }

        // resetCooldown(skill, player) - 重置指定玩家的技能冷却
        runtime.registerFunction("resetCooldown", returns(Type.VOID).params(Type.OBJECT, Type.OBJECT)) { ctx ->
            val skill = resolveSkill(ctx.getRef(0)) ?: return@registerFunction
            val player = ctx.getPlayerArg(1) ?: return@registerFunction
            Cooler.INSTANCE.set(player, skill, 0)
        }

        // hasCooldown(skill) - 检查 sender 是否有冷却
        runtime.registerFunction("hasCooldown", returns(Type.BOOLEAN).params(Type.OBJECT)) { ctx ->
            val skill = resolveSkill(ctx.getRef(0)) ?: return@registerFunction
            val player = ctx.getPlayerArg(-1) ?: return@registerFunction
            ctx.setReturnBool(Cooler.INSTANCE.get(player, skill) > 0)
        }

        // hasCooldown(skill, player) - 检查指定玩家是否有冷却
        runtime.registerFunction("hasCooldown", returns(Type.BOOLEAN).params(Type.OBJECT, Type.OBJECT)) { ctx ->
            val skill = resolveSkill(ctx.getRef(0)) ?: return@registerFunction
            val player = ctx.getPlayerArg(1) ?: return@registerFunction
            ctx.setReturnBool(Cooler.INSTANCE.get(player, skill) > 0)
        }
    }
}
