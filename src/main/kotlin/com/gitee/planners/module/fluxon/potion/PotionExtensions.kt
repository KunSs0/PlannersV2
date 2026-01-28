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

        // potion(type, level, duration) - 给 sender 添加药水效果
        runtime.registerFunction("potion", returns(Type.VOID).params(Type.STRING, Type.NUMBER, Type.NUMBER)) { ctx ->
            val typeName = ctx.getString(0) ?: return@registerFunction
            val level = ctx.getAsInt(1)
            val duration = ctx.getAsInt(2)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            applyPotion(typeName, level, duration, targets)
            null
        }

        // potion(type, level, duration, targets) - 给目标添加药水效果
        runtime.registerFunction("potion", returns(Type.VOID).params(Type.STRING, Type.NUMBER, Type.NUMBER, Type.OBJECT)) { ctx ->
            val typeName = ctx.getString(0) ?: return@registerFunction
            val level = ctx.getAsInt(1)
            val duration = ctx.getAsInt(2)
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)
            applyPotion(typeName, level, duration, targets)
            null
        }

        // potionRemove(type) - 移除 sender 的药水效果
        runtime.registerFunction("potionRemove", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val typeName = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            removePotion(typeName, targets)
            null
        }

        // potionRemove(type, targets) - 移除目标的药水效果
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
