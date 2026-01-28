package com.gitee.planners.module.fluxon.velocity

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.getTargetsArg
import com.gitee.planners.module.fluxon.registerFunction
import org.bukkit.entity.Entity
import org.bukkit.util.Vector
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 实体速度控制扩展
 */
object VelocityExtensions {

    @Awake(LifeCycle.LOAD)
    fun init() {
        val runtime = FluxonScriptCache.runtime

        // velocitySet(x, y, z, [targets]) - 设置目标速度
        runtime.registerFunction("velocitySet", listOf(3, 4)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.velocity = Vector(x, y, z)
            }
            null
        }

        // velocityAdd(x, y, z, [targets]) - 增加目标速度
        runtime.registerFunction("velocityAdd", listOf(3, 4)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.velocity = target.instance.velocity.add(Vector(x, y, z))
            }
            null
        }

        // velocityMove(x, y, z, [targets]) - 根据实体朝向移动
        runtime.registerFunction("velocityMove", listOf(3, 4)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val direction = target.instance.location.direction
                val velocity = direction.multiply(z).add(Vector(0.0, y, 0.0))
                    .add(direction.clone().rotateAroundY(Math.PI / 2).multiply(x))
                target.instance.velocity = target.instance.velocity.add(velocity)
            }
            null
        }

        // velocityZero([targets]) - 清除目标速度
        runtime.registerFunction("velocityZero", listOf(0, 1)) { ctx ->
            val targets = ctx.getTargetsArg(0, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.velocity = Vector(0, 0, 0)
            }
            null
        }

        // getVelocity(entity) - 获取实体速度
        runtime.registerFunction("getVelocity", listOf(1)) { ctx ->
            val entity = ctx.getRef(0) as? Entity ?: return@registerFunction Vector()
            entity.velocity
        }
    }
}
