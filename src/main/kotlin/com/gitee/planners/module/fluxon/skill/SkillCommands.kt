package com.gitee.planners.module.fluxon.skill

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache

import com.gitee.planners.module.fluxon.getTargetsArg
import com.gitee.planners.module.fluxon.resolveLivingEntity
import org.bukkit.entity.LivingEntity
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.platform.util.setMeta

/**
 * 技能相关全局函数
 */
object SkillCommands {

    @Awake(LifeCycle.LOAD)
    fun init() {
        val runtime = FluxonScriptCache.runtime

        // damage(amount) - 对 sender 造成伤害
        runtime.registerFunction("damage", returns(Type.VOID).params(Type.NUMBER)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                (target.instance as? LivingEntity)?.damage(amount)
            }
            null
        }

        // damage(amount, targets) - 对目标造成伤害
        runtime.registerFunction("damage", returns(Type.VOID).params(Type.NUMBER, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                (target.instance as? LivingEntity)?.damage(amount)
            }
            null
        }

        // damageBy(amount, source) - 以来源对 sender 造成伤害
        runtime.registerFunction("damageBy", returns(Type.VOID).params(Type.NUMBER, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val source = ctx.getRef(1)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            val killer = resolveLivingEntity(source)
            applyDamageBy(amount, killer, targets)
            null
        }

        // damageBy(amount, source, targets) - 以来源对目标造成伤害
        runtime.registerFunction("damageBy", returns(Type.VOID).params(Type.NUMBER, Type.OBJECT, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val source = ctx.getRef(1)
            val targets = ctx.getTargetsArg(2, LeastType.SENDER)
            val killer = resolveLivingEntity(source)
            applyDamageBy(amount, killer, targets)
            null
        }

        // heal(amount) - 治疗 sender
        runtime.registerFunction("heal", returns(Type.VOID).params(Type.NUMBER)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            applyHeal(amount, targets)
            null
        }

        // heal(amount, targets) - 治疗目标
        runtime.registerFunction("heal", returns(Type.VOID).params(Type.NUMBER, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            applyHeal(amount, targets)
            null
        }
    }

    private fun applyDamageBy(amount: Double, killer: LivingEntity?, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val entity = target.instance as? LivingEntity ?: return@forEach
            if (killer != null && killer != entity && entity.health <= amount) {
                entity.setMeta("@killer", killer)
            }
            entity.damage(amount)
        }
    }

    private fun applyHeal(amount: Double, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val entity = target.instance as? LivingEntity ?: return@forEach
            @Suppress("DEPRECATION")
            entity.health = (entity.health + amount).coerceAtMost(entity.maxHealth)
        }
    }
}
