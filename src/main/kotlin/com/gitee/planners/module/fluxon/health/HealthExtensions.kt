package com.gitee.planners.module.fluxon.health

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.getTargetsArg
import com.gitee.planners.module.fluxon.registerFunction
import org.bukkit.entity.LivingEntity
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 生命值操作扩展
 */
object HealthExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // healthAdd(amount, [targets]) - 增加生命值
        runtime.registerFunction("healthAdd", listOf(1, 2)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val entity = target.instance as? LivingEntity ?: return@forEach
                @Suppress("DEPRECATION")
                entity.health = (entity.health + amount).coerceAtMost(entity.maxHealth)
            }
            null
        }

        // healthSet(amount, [targets]) - 设置生命值
        runtime.registerFunction("healthSet", listOf(1, 2)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val entity = target.instance as? LivingEntity ?: return@forEach
                @Suppress("DEPRECATION")
                entity.health = amount.coerceIn(0.0, entity.maxHealth)
            }
            null
        }

        // healthTake(amount, [targets]) - 减少生命值
        runtime.registerFunction("healthTake", listOf(1, 2)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val entity = target.instance as? LivingEntity ?: return@forEach
                entity.health = (entity.health - amount).coerceAtLeast(0.0)
            }
            null
        }
    }
}
