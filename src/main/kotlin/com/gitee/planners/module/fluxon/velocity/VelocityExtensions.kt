package com.gitee.planners.module.fluxon.velocity

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache

import com.gitee.planners.module.fluxon.getTargetsArg
import org.bukkit.entity.Entity
import org.bukkit.util.Vector
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 实体速度控制扩展
 */
object VelocityExtensions {

    @Awake(LifeCycle.LOAD)
    fun init() {
        val runtime = FluxonScriptCache.runtime

        // velocitySet(x, y, z) - 设置 sender 速度
        runtime.registerFunction("velocitySet", returns(Type.VOID).params(Type.NUMBER, Type.NUMBER, Type.NUMBER)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.velocity = Vector(x, y, z)
            }
        }

        // velocitySet(x, y, z, targets) - 设置目标速度
        runtime.registerFunction("velocitySet", returns(Type.VOID).params(Type.NUMBER, Type.NUMBER, Type.NUMBER, Type.OBJECT)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.velocity = Vector(x, y, z)
            }
        }

        // velocityAdd(x, y, z) - 增加 sender 速度
        runtime.registerFunction("velocityAdd", returns(Type.VOID).params(Type.NUMBER, Type.NUMBER, Type.NUMBER)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.velocity = target.instance.velocity.add(Vector(x, y, z))
            }
        }

        // velocityAdd(x, y, z, targets) - 增加目标速度
        runtime.registerFunction("velocityAdd", returns(Type.VOID).params(Type.NUMBER, Type.NUMBER, Type.NUMBER, Type.OBJECT)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.velocity = target.instance.velocity.add(Vector(x, y, z))
            }
        }

        // velocityMove(x, y, z) - 根据 sender 朝向移动
        runtime.registerFunction("velocityMove", returns(Type.VOID).params(Type.NUMBER, Type.NUMBER, Type.NUMBER)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            applyVelocityMove(targets, x, y, z)
        }

        // velocityMove(x, y, z, targets) - 根据实体朝向移动
        runtime.registerFunction("velocityMove", returns(Type.VOID).params(Type.NUMBER, Type.NUMBER, Type.NUMBER, Type.OBJECT)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)
            applyVelocityMove(targets, x, y, z)
        }

        // velocityZero() - 清除 sender 速度
        runtime.registerFunction("velocityZero", returns(Type.VOID).noParams()) { ctx ->
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.velocity = Vector(0, 0, 0)
            }
        }

        // velocityZero(targets) - 清除目标速度
        runtime.registerFunction("velocityZero", returns(Type.VOID).params(Type.OBJECT)) { ctx ->
            val targets = ctx.getTargetsArg(0, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.velocity = Vector(0, 0, 0)
            }
        }

        // getVelocity(entity) - 获取实体速度
        runtime.registerFunction("getVelocity", returns(Type.OBJECT).params(Type.OBJECT)) { ctx ->
            val entity = ctx.getRef(0) as? Entity ?: return@registerFunction
            ctx.setReturnRef(entity.velocity)
        }
    }

    private fun applyVelocityMove(targets: com.gitee.planners.api.job.target.ProxyTargetContainer, x: Double, y: Double, z: Double) {
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val direction = target.instance.location.direction
            val velocity = direction.multiply(z).add(Vector(0.0, y, 0.0))
                .add(direction.clone().rotateAroundY(Math.PI / 2).multiply(x))
            target.instance.velocity = target.instance.velocity.add(velocity)
        }
    }
}
