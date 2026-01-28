package com.gitee.planners.module.fluxon.skill

import com.gitee.planners.api.damage.DamageCause
import com.gitee.planners.api.damage.ProxyDamage
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.ProxyTargetContainer
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.getTargetsArg
import com.gitee.planners.module.fluxon.resolveLivingEntity
import org.bukkit.entity.LivingEntity
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 技能相关全局函数
 */
object SkillCommands {

    @Awake(LifeCycle.LOAD)
    fun init() {
        val runtime = FluxonScriptCache.runtime

        // damage(amount) - 对 sender 造成伤害
        runtime.registerFunction("damage", returns(Type.VOID).params(Type.D)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            applyDamage(amount, null, DamageCause.of("SKILL"), targets)
            null
        }

        // damage(amount, targets) - 对目标造成伤害
        runtime.registerFunction("damage", returns(Type.VOID).params(Type.D, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            applyDamage(amount, null, DamageCause.of("SKILL"), targets)
            null
        }

        // damageBy(amount, source) - 以来源对 sender 造成伤害
        runtime.registerFunction("damageBy", returns(Type.VOID).params(Type.D, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val source = ctx.getRef(1)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            val killer = resolveLivingEntity(source)
            applyDamage(amount, killer, DamageCause.of("SKILL"), targets)
            null
        }

        // damageBy(amount, source, targets) - 以来源对目标造成伤害
        runtime.registerFunction("damageBy", returns(Type.VOID).params(Type.D, Type.OBJECT, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val source = ctx.getRef(1)
            val targets = ctx.getTargetsArg(2, LeastType.SENDER)
            val killer = resolveLivingEntity(source)
            applyDamage(amount, killer, DamageCause.of("SKILL"), targets)
            null
        }

        // damageEx(amount, cause) - 对 sender 造成指定类型伤害
        runtime.registerFunction("damageEx", returns(Type.VOID).params(Type.D, Type.STRING)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val causeName = ctx.getString(1)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            applyDamage(amount, null, DamageCause.of(causeName), targets)
            null
        }

        // damageEx(amount, cause, targets) - 对目标造成指定类型伤害
        runtime.registerFunction("damageEx", returns(Type.VOID).params(Type.D, Type.STRING, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val causeName = ctx.getString(1)
            val targets = ctx.getTargetsArg(2, LeastType.SENDER)
            applyDamage(amount, null, DamageCause.of(causeName), targets)
            null
        }

        // damageExBy(amount, cause, source) - 以来源对 sender 造成指定类型伤害
        runtime.registerFunction("damageExBy", returns(Type.VOID).params(Type.D, Type.STRING, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val causeName = ctx.getString(1)
            val source = ctx.getRef(2)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            val killer = resolveLivingEntity(source)
            applyDamage(amount, killer, DamageCause.of(causeName), targets)
            null
        }

        // damageExBy(amount, cause, source, targets) - 以来源对目标造成指定类型伤害
        runtime.registerFunction("damageExBy", returns(Type.VOID).params(Type.D, Type.STRING, Type.OBJECT, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val causeName = ctx.getString(1)
            val source = ctx.getRef(2)
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)
            val killer = resolveLivingEntity(source)
            applyDamage(amount, killer, DamageCause.of(causeName), targets)
            null
        }

        // heal(amount) - 治疗 sender
        runtime.registerFunction("heal", returns(Type.VOID).params(Type.D)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            applyHeal(amount, targets)
            null
        }

        // heal(amount, targets) - 治疗目标
        runtime.registerFunction("heal", returns(Type.VOID).params(Type.D, Type.OBJECT)) { ctx ->
            val amount = ctx.getAsDouble(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            applyHeal(amount, targets)
            null
        }
    }

    private fun applyDamage(amount: Double, source: LivingEntity?, cause: DamageCause, targets: ProxyTargetContainer) {
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val entity = target.instance as? LivingEntity ?: return@forEach
            ProxyDamage.builder()
                .source(source)
                .target(entity)
                .damage(amount)
                .cause(cause)
                .build()
                .execute()
        }
    }

    private fun applyHeal(amount: Double, targets: ProxyTargetContainer) {
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val entity = target.instance as? LivingEntity ?: return@forEach
            @Suppress("DEPRECATION")
            entity.health = (entity.health + amount).coerceAtMost(entity.maxHealth)
        }
    }
}
