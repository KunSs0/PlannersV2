package com.gitee.planners.module.fluxon.potion

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.getTargetsArg
import com.gitee.planners.module.fluxon.registerFunction
import org.bukkit.entity.LivingEntity
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 药水效果扩展
 */
object PotionExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // potion(type, level, duration, [targets]) - 添加药水效果
        runtime.registerFunction("potion", listOf(3, 4)) { ctx ->
            val typeName = ctx.getAsString(0) ?: return@registerFunction null
            val level = ctx.getAsInt(1)
            val duration = ctx.getAsInt(2)
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)

            val type = PotionEffectType.getByName(typeName) ?: return@registerFunction null
            val effect = PotionEffect(type, duration, level - 1, false, true)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                (target.instance as? LivingEntity)?.addPotionEffect(effect)
            }
            null
        }

        // potionRemove(type, [targets]) - 移除药水效果
        runtime.registerFunction("potionRemove", listOf(1, 2)) { ctx ->
            val typeName = ctx.getAsString(0) ?: return@registerFunction null
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)

            val type = PotionEffectType.getByName(typeName) ?: return@registerFunction null

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                (target.instance as? LivingEntity)?.removePotionEffect(type)
            }
            null
        }
    }
}
