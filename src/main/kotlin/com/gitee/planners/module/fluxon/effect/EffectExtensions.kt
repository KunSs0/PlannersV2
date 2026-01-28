package com.gitee.planners.module.fluxon.effect

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache

import com.gitee.planners.module.fluxon.getTargetsArg
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 实体状态效果扩展
 */
object EffectExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // freeze(ticks) - 冻结 sender
        runtime.registerFunction("freeze", returns(Type.VOID).params(Type.NUMBER)) { ctx ->
            val ticks = ctx.getAsInt(0)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.freezeTicks = ticks
            }
            null
        }

        // freeze(ticks, targets) - 冻结目标
        runtime.registerFunction("freeze", returns(Type.VOID).params(Type.NUMBER, Type.OBJECT)) { ctx ->
            val ticks = ctx.getAsInt(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.freezeTicks = ticks
            }
            null
        }

        // fire(ticks) - 点燃 sender
        runtime.registerFunction("fire", returns(Type.VOID).params(Type.NUMBER)) { ctx ->
            val ticks = ctx.getAsInt(0)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.fireTicks = ticks
            }
            null
        }

        // fire(ticks, targets) - 点燃目标
        runtime.registerFunction("fire", returns(Type.VOID).params(Type.NUMBER, Type.OBJECT)) { ctx ->
            val ticks = ctx.getAsInt(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.fireTicks = ticks
            }
            null
        }

        // explosion(power) - 在 origin 创建爆炸
        runtime.registerFunction("explosion", returns(Type.VOID).params(Type.NUMBER)) { ctx ->
            val power = ctx.getAsDouble(0).toFloat()
            val targets = ctx.getTargetsArg(-1, LeastType.ORIGIN)
            createExplosion(power, false, false, targets)
            null
        }

        // explosion(power, fire) - 创建爆炸，可选火焰
        runtime.registerFunction("explosion", returns(Type.VOID).params(Type.NUMBER, Type.BOOLEAN)) { ctx ->
            val power = ctx.getAsDouble(0).toFloat()
            val setFire = ctx.getBool(1)
            val targets = ctx.getTargetsArg(-1, LeastType.ORIGIN)
            createExplosion(power, setFire, false, targets)
            null
        }

        // explosion(power, fire, break) - 创建爆炸，可选火焰和破坏
        runtime.registerFunction("explosion", returns(Type.VOID).params(Type.NUMBER, Type.BOOLEAN, Type.BOOLEAN)) { ctx ->
            val power = ctx.getAsDouble(0).toFloat()
            val setFire = ctx.getBool(1)
            val breakBlocks = ctx.getBool(2)
            val targets = ctx.getTargetsArg(-1, LeastType.ORIGIN)
            createExplosion(power, setFire, breakBlocks, targets)
            null
        }

        // explosion(power, fire, break, locations) - 在指定位置创建爆炸
        runtime.registerFunction("explosion", returns(Type.VOID).params(Type.NUMBER, Type.BOOLEAN, Type.BOOLEAN, Type.OBJECT)) { ctx ->
            val power = ctx.getAsDouble(0).toFloat()
            val setFire = ctx.getBool(1)
            val breakBlocks = ctx.getBool(2)
            val targets = ctx.getTargetsArg(3, LeastType.ORIGIN)
            createExplosion(power, setFire, breakBlocks, targets)
            null
        }
    }

    private fun createExplosion(power: Float, setFire: Boolean, breakBlocks: Boolean, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        targets.filterIsInstance<ProxyTarget.Location<*>>().forEach { target ->
            val location = target.getBukkitLocation()
            location.world?.createExplosion(location, power, setFire, breakBlocks)
        }
    }
}
