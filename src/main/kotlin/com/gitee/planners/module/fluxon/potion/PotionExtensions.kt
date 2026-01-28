package com.gitee.planners.module.fluxon.potion

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache

import com.gitee.planners.module.fluxon.getTargetsArg
import org.bukkit.entity.LivingEntity
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 药水效果扩展
 */
object PotionExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        /**
         * 给 sender 添加药水效果
         * @param type 药水效果类型名称（如 SPEED, STRENGTH, REGENERATION）
         * @param level 效果等级（1=I级，2=II级...）
         * @param duration 持续时间（tick，20 ticks = 1 秒）
         */
        runtime.registerFunction("potion", returns(Type.VOID).params(Type.STRING, Type.NUMBER, Type.NUMBER)) { ctx ->
            val typeName = ctx.getString(0) ?: return@registerFunction
            val level = ctx.getAsInt(1)
            val duration = ctx.getAsInt(2)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            applyPotion(typeName, level, duration, targets)
            null
        }

        /**
         * 给目标添加药水效果
         * @param type 药水效果类型名称
         * @param level 效果等级
         * @param duration 持续时间（tick）
         * @param targets 目标实体（必须是 LivingEntity）
         */
        runtime.registerFunction("potion", returns(Type.VOID).params(Type.STRING, Type.NUMBER, Type.NUMBER, Type.OBJECT)) { ctx ->
            val typeName = ctx.getString(0) ?: return@registerFunction
            val level = ctx.getAsInt(1)
            val duration = ctx.getAsInt(2)
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)
            applyPotion(typeName, level, duration, targets)
            null
        }

        /**
         * 移除 sender 的指定药水效果
         * @param type 药水效果类型名称
         */
        runtime.registerFunction("potionRemove", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val typeName = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            removePotion(typeName, targets)
            null
        }

        /**
         * 移除目标的指定药水效果
         * @param type 药水效果类型名称
         * @param targets 目标实体
         */
        runtime.registerFunction("potionRemove", returns(Type.VOID).params(Type.STRING, Type.OBJECT)) { ctx ->
            val typeName = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            removePotion(typeName, targets)
            null
        }
    }

    private fun applyPotion(typeName: String, level: Int, duration: Int, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        val type = PotionEffectType.getByName(typeName) ?: return
        val effect = PotionEffect(type, duration, level - 1, false, true)
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            (target.instance as? LivingEntity)?.addPotionEffect(effect)
        }
    }

    private fun removePotion(typeName: String, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        val type = PotionEffectType.getByName(typeName) ?: return
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            (target.instance as? LivingEntity)?.removePotionEffect(type)
        }
    }
}
