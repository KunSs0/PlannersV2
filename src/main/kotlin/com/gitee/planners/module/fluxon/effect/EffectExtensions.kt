package com.gitee.planners.module.fluxon.effect

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.getTargetsArg
import com.gitee.planners.module.fluxon.registerFunction
import org.bukkit.entity.Entity
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 实体状态效果扩展
 */
object EffectExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // freeze(ticks, [targets]) - 冻结实体
        runtime.registerFunction("freeze", listOf(1, 2)) { ctx ->
            val ticks = ctx.getAsInt(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.freezeTicks = ticks
            }
            null
        }

        // fire(ticks, [targets]) - 点燃实体
        runtime.registerFunction("fire", listOf(1, 2)) { ctx ->
            val ticks = ctx.getAsInt(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.fireTicks = ticks
            }
            null
        }

        // explosion(power, [fire], [break], [locations]) - 创建爆炸
        runtime.registerFunction("explosion", listOf(1, 2, 3, 4)) { ctx ->
            val power = ctx.getAsDouble(0).toFloat()
            val setFire = if (ctx.arguments.size > 1) ctx.getRef(1) as? Boolean ?: false else false
            val breakBlocks = if (ctx.arguments.size > 2) ctx.getRef(2) as? Boolean ?: false else false
            val targets = ctx.getTargetsArg(3, LeastType.ORIGIN)

            targets.filterIsInstance<ProxyTarget.Location<*>>().forEach { target ->
                val location = target.getBukkitLocation()
                location.world?.createExplosion(location, power, setFire, breakBlocks)
            }
            null
        }
    }
}
