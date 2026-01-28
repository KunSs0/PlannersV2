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

        /**
         * 获取 sender 的技能剩余冷却时间
         * @param skill 技能对象或技能 ID 字符串
         * @return 剩余冷却 tick 数
         */
        runtime.registerFunction("getCooldown", returns(Type.J).params(Type.OBJECT)) { ctx ->
            val skill = resolveSkill(ctx.getRef(0)) ?: return@registerFunction
            val player = ctx.getPlayerArg(-1) ?: return@registerFunction
            ctx.setReturnLong(Cooler.INSTANCE.get(player, skill))
        }

        /**
         * 获取指定玩家的技能剩余冷却时间
         * @param skill 技能对象或技能 ID 字符串
         * @param player 目标玩家
         * @return 剩余冷却 tick 数
         */
        runtime.registerFunction("getCooldown", returns(Type.J).params(Type.OBJECT, Type.OBJECT)) { ctx ->
            val skill = resolveSkill(ctx.getRef(0)) ?: return@registerFunction
            val player = ctx.getPlayerArg(1) ?: return@registerFunction
            ctx.setReturnLong(Cooler.INSTANCE.get(player, skill))
        }

        /**
         * 设置 sender 的技能冷却时间
         * @param skill 技能对象或技能 ID 字符串
         * @param ticks 冷却时间（tick，20 ticks = 1 秒）
         */
        runtime.registerFunction("setCooldown", returns(Type.VOID).params(Type.OBJECT, Type.I)) { ctx ->
            val skill = resolveSkill(ctx.getRef(0)) ?: return@registerFunction
            val ticks = ctx.getAsInt(1)
            val player = ctx.getPlayerArg(-1) ?: return@registerFunction
            Cooler.INSTANCE.set(player, skill, ticks)
        }

        /**
         * 设置指定玩家的技能冷却时间
         * @param skill 技能对象或技能 ID 字符串
         * @param ticks 冷却时间（tick）
         * @param player 目标玩家
         */
        runtime.registerFunction("setCooldown", returns(Type.VOID).params(Type.OBJECT, Type.I, Type.OBJECT)) { ctx ->
            val skill = resolveSkill(ctx.getRef(0)) ?: return@registerFunction
            val ticks = ctx.getAsInt(1)
            val player = ctx.getPlayerArg(2) ?: return@registerFunction
            Cooler.INSTANCE.set(player, skill, ticks)
        }

        /**
         * 重置 sender 的技能冷却（设为 0）
         * @param skill 技能对象或技能 ID 字符串
         */
        runtime.registerFunction("resetCooldown", returns(Type.VOID).params(Type.OBJECT)) { ctx ->
            val skill = resolveSkill(ctx.getRef(0)) ?: return@registerFunction
            val player = ctx.getPlayerArg(-1) ?: return@registerFunction
            Cooler.INSTANCE.set(player, skill, 0)
        }

        /**
         * 重置指定玩家的技能冷却（设为 0）
         * @param skill 技能对象或技能 ID 字符串
         * @param player 目标玩家
         */
        runtime.registerFunction("resetCooldown", returns(Type.VOID).params(Type.OBJECT, Type.OBJECT)) { ctx ->
            val skill = resolveSkill(ctx.getRef(0)) ?: return@registerFunction
            val player = ctx.getPlayerArg(1) ?: return@registerFunction
            Cooler.INSTANCE.set(player, skill, 0)
        }

        /**
         * 检查 sender 是否有技能冷却
         * @param skill 技能对象或技能 ID 字符串
         * @return 是否正在冷却中
         */
        runtime.registerFunction("hasCooldown", returns(Type.BOOLEAN).params(Type.OBJECT)) { ctx ->
            val skill = resolveSkill(ctx.getRef(0)) ?: return@registerFunction
            val player = ctx.getPlayerArg(-1) ?: return@registerFunction
            ctx.setReturnBool(Cooler.INSTANCE.get(player, skill) > 0)
        }

        /**
         * 检查指定玩家是否有技能冷却
         * @param skill 技能对象或技能 ID 字符串
         * @param player 目标玩家
         * @return 是否正在冷却中
         */
        runtime.registerFunction("hasCooldown", returns(Type.BOOLEAN).params(Type.OBJECT, Type.OBJECT)) { ctx ->
            val skill = resolveSkill(ctx.getRef(0)) ?: return@registerFunction
            val player = ctx.getPlayerArg(1) ?: return@registerFunction
            ctx.setReturnBool(Cooler.INSTANCE.get(player, skill) > 0)
        }
    }
}
