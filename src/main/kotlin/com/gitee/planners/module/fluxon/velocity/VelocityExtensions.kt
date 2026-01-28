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

        /**
         * 设置 sender 的速度向量
         * @param x X 轴速度分量
         * @param y Y 轴速度分量（垂直方向）
         * @param z Z 轴速度分量
         */
        runtime.registerFunction("velocitySet", returns(Type.VOID).params(Type.NUMBER, Type.NUMBER, Type.NUMBER)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.velocity = Vector(x, y, z)
            }
        }

        /**
         * 设置目标的速度向量
         * @param x X 轴速度分量
         * @param y Y 轴速度分量
         * @param z Z 轴速度分量
         * @param targets 目标实体
         */
        runtime.registerFunction("velocitySet", returns(Type.VOID).params(Type.NUMBER, Type.NUMBER, Type.NUMBER, Type.OBJECT)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.velocity = Vector(x, y, z)
            }
        }

        /**
         * 在 sender 当前速度基础上增加速度
         * @param x X 轴速度增量
         * @param y Y 轴速度增量
         * @param z Z 轴速度增量
         */
        runtime.registerFunction("velocityAdd", returns(Type.VOID).params(Type.NUMBER, Type.NUMBER, Type.NUMBER)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.velocity = target.instance.velocity.add(Vector(x, y, z))
            }
        }

        /**
         * 在目标当前速度基础上增加速度
         * @param x X 轴速度增量
         * @param y Y 轴速度增量
         * @param z Z 轴速度增量
         * @param targets 目标实体
         */
        runtime.registerFunction("velocityAdd", returns(Type.VOID).params(Type.NUMBER, Type.NUMBER, Type.NUMBER, Type.OBJECT)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.velocity = target.instance.velocity.add(Vector(x, y, z))
            }
        }

        /**
         * 根据 sender 朝向添加相对速度
         * @param x 左右速度（正=右，负=左）
         * @param y 垂直速度（正=上，负=下）
         * @param z 前后速度（正=前，负=后）
         */
        runtime.registerFunction("velocityMove", returns(Type.VOID).params(Type.NUMBER, Type.NUMBER, Type.NUMBER)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            applyVelocityMove(targets, x, y, z)
        }

        /**
         * 根据目标朝向添加相对速度
         * @param x 左右速度
         * @param y 垂直速度
         * @param z 前后速度
         * @param targets 目标实体
         */
        runtime.registerFunction("velocityMove", returns(Type.VOID).params(Type.NUMBER, Type.NUMBER, Type.NUMBER, Type.OBJECT)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)
            applyVelocityMove(targets, x, y, z)
        }

        /**
         * 清除 sender 的速度（设为零向量）
         */
        runtime.registerFunction("velocityZero", returns(Type.VOID).noParams()) { ctx ->
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.velocity = Vector(0, 0, 0)
            }
        }

        /**
         * 清除目标的速度
         * @param targets 目标实体
         */
        runtime.registerFunction("velocityZero", returns(Type.VOID).params(Type.OBJECT)) { ctx ->
            val targets = ctx.getTargetsArg(0, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.velocity = Vector(0, 0, 0)
            }
        }

        /**
         * 获取实体当前速度向量
         * @param entity 目标实体
         * @return 速度向量 (org.bukkit.util.Vector)
         */
        runtime.registerFunction("getVelocity", returns(Type.OBJECT).params(Type.OBJECT)) { ctx ->
            val entity = ctx.getRef(0) as? Entity ?: return@registerFunction
            ctx.setReturnRef(entity.velocity)
        }
    }

    /**
     * 根据目标朝向计算并应用相对速度
     * @param targets 目标容器
     * @param x 左右速度（正=右，负=左）
     * @param y 垂直速度（正=上，负=下）
     * @param z 前后速度（正=前，负=后）
     */
    private fun applyVelocityMove(targets: com.gitee.planners.api.job.target.ProxyTargetContainer, x: Double, y: Double, z: Double) {
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val direction = target.instance.location.direction
            val velocity = direction.multiply(z).add(Vector(0.0, y, 0.0))
                .add(direction.clone().rotateAroundY(Math.PI / 2).multiply(x))
            target.instance.velocity = target.instance.velocity.add(velocity)
        }
    }
}
