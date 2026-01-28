package com.gitee.planners.module.fluxon.health

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache

import com.gitee.planners.module.fluxon.getTargetsArg
import org.bukkit.entity.LivingEntity
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 生命值操作扩展
 */
object HealthExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        /**
         * 增加 sender 的生命值（不超过最大值）
         * @param amount 增加的生命值数量
         */
        runtime.registerFunction("healthAdd", returns(Type.VOID).params(Type.NUMBER)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            addHealth(targets, amount)
            null
        }

        /**
         * 增加目标的生命值
         * @param amount 增加的生命值数量
         * @param targets 目标实体（支持 Entity/ProxyTarget/容器）
         */
        runtime.registerFunction("healthAdd", returns(Type.VOID).params(Type.NUMBER, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            addHealth(targets, amount)
            null
        }

        /**
         * 设置 sender 的生命值（限制在 0 到最大值之间）
         * @param amount 目标生命值
         */
        runtime.registerFunction("healthSet", returns(Type.VOID).params(Type.NUMBER)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            setHealth(targets, amount)
            null
        }

        /**
         * 设置目标的生命值
         * @param amount 目标生命值
         * @param targets 目标实体
         */
        runtime.registerFunction("healthSet", returns(Type.VOID).params(Type.NUMBER, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            setHealth(targets, amount)
            null
        }

        /**
         * 减少 sender 的生命值（不低于 0，不触发伤害事件）
         * @param amount 减少的生命值数量
         */
        runtime.registerFunction("healthTake", returns(Type.VOID).params(Type.NUMBER)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            takeHealth(targets, amount)
            null
        }

        /**
         * 减少目标的生命值
         * @param amount 减少的生命值数量
         * @param targets 目标实体
         */
        runtime.registerFunction("healthTake", returns(Type.VOID).params(Type.NUMBER, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            takeHealth(targets, amount)
            null
        }
    }

    private fun addHealth(targets: com.gitee.planners.api.job.target.ProxyTargetContainer, amount: Double) {
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val entity = target.instance as? LivingEntity ?: return@forEach
            @Suppress("DEPRECATION")
            entity.health = (entity.health + amount).coerceAtMost(entity.maxHealth)
        }
    }

    private fun setHealth(targets: com.gitee.planners.api.job.target.ProxyTargetContainer, amount: Double) {
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val entity = target.instance as? LivingEntity ?: return@forEach
            @Suppress("DEPRECATION")
            entity.health = amount.coerceIn(0.0, entity.maxHealth)
        }
    }

    private fun takeHealth(targets: com.gitee.planners.api.job.target.ProxyTargetContainer, amount: Double) {
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val entity = target.instance as? LivingEntity ?: return@forEach
            entity.health = (entity.health - amount).coerceAtLeast(0.0)
        }
    }
}
