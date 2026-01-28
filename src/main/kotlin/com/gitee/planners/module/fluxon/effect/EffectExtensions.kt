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

        /**
         * 冻结 sender（细雪冻结效果）
         * @param ticks 冻结时间（tick），最大 140 ticks 达到最大冻结效果
         */
        runtime.registerFunction("freeze", returns(Type.VOID).params(Type.NUMBER)) { ctx ->
            val ticks = ctx.getAsInt(0)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.freezeTicks = ticks
            }
            null
        }

        /**
         * 冻结目标
         * @param ticks 冻结时间（tick）
         * @param targets 目标实体
         */
        runtime.registerFunction("freeze", returns(Type.VOID).params(Type.NUMBER, Type.OBJECT)) { ctx ->
            val ticks = ctx.getAsInt(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.freezeTicks = ticks
            }
            null
        }

        /**
         * 点燃 sender
         * @param ticks 燃烧时间（tick，20 ticks = 1 秒）
         */
        runtime.registerFunction("fire", returns(Type.VOID).params(Type.NUMBER)) { ctx ->
            val ticks = ctx.getAsInt(0)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.fireTicks = ticks
            }
            null
        }

        /**
         * 点燃目标
         * @param ticks 燃烧时间（tick）
         * @param targets 目标实体
         */
        runtime.registerFunction("fire", returns(Type.VOID).params(Type.NUMBER, Type.OBJECT)) { ctx ->
            val ticks = ctx.getAsInt(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.instance.fireTicks = ticks
            }
            null
        }

        /**
         * 在 origin 位置创建爆炸（无火焰，不破坏方块）
         * @param power 爆炸威力（TNT=4, 苦力怕=3, 末影水晶=6）
         */
        runtime.registerFunction("explosion", returns(Type.VOID).params(Type.NUMBER)) { ctx ->
            val power = ctx.getAsDouble(0).toFloat()
            val targets = ctx.getTargetsArg(-1, LeastType.ORIGIN)
            createExplosion(power, false, false, targets)
            null
        }

        /**
         * 创建爆炸，可选是否产生火焰
         * @param power 爆炸威力
         * @param fire 是否产生火焰
         */
        runtime.registerFunction("explosion", returns(Type.VOID).params(Type.NUMBER, Type.BOOLEAN)) { ctx ->
            val power = ctx.getAsDouble(0).toFloat()
            val setFire = ctx.getBool(1)
            val targets = ctx.getTargetsArg(-1, LeastType.ORIGIN)
            createExplosion(power, setFire, false, targets)
            null
        }

        /**
         * 创建爆炸，可选火焰和方块破坏
         * @param power 爆炸威力
         * @param fire 是否产生火焰
         * @param break 是否破坏方块
         */
        runtime.registerFunction("explosion", returns(Type.VOID).params(Type.NUMBER, Type.BOOLEAN, Type.BOOLEAN)) { ctx ->
            val power = ctx.getAsDouble(0).toFloat()
            val setFire = ctx.getBool(1)
            val breakBlocks = ctx.getBool(2)
            val targets = ctx.getTargetsArg(-1, LeastType.ORIGIN)
            createExplosion(power, setFire, breakBlocks, targets)
            null
        }

        /**
         * 在指定位置创建爆炸
         * @param power 爆炸威力
         * @param fire 是否产生火焰
         * @param break 是否破坏方块
         * @param locations 爆炸位置（支持 Location/ProxyTarget）
         */
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
