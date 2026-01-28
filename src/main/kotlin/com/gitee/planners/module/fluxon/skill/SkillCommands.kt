package com.gitee.planners.module.fluxon.skill

import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.registerFunction
import org.bukkit.entity.LivingEntity
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.platform.util.setMeta

/**
 * 技能相关全局函数注册
 */
@Awake(LifeCycle.LOAD)
private fun registerSkillCommands() {
    val runtime = FluxonScriptCache.runtime

    // damage(amount, target)
    runtime.registerFunction("damage", listOf(2)) { ctx ->
        val amount = ctx.getAsDouble(0)
        val target = ctx.getRef(1) as LivingEntity
        target.damage(amount)
        null
    }

    // damageWithSource(amount, source, target)
    runtime.registerFunction("damageWithSource", listOf(3)) { ctx ->
        val amount = ctx.getAsDouble(0)
        val source = ctx.getRef(1)
        val target = ctx.getRef(2) as LivingEntity
        val killer = resolveLivingEntity(source)
        if (killer != null && killer != target && target.health <= amount) {
            target.setMeta("@killer", killer)
        }
        target.damage(amount)
        null
    }

    // heal(amount, target)
    runtime.registerFunction("heal", listOf(2)) { ctx ->
        val amount = ctx.getAsDouble(0)
        val target = ctx.getRef(1) as LivingEntity
        @Suppress("DEPRECATION")
        target.health = (target.health + amount).coerceAtMost(target.maxHealth)
        null
    }
}

private fun resolveLivingEntity(arg: Any?): LivingEntity? {
    return when (arg) {
        is ProxyTarget.BukkitEntity -> arg.instance as? LivingEntity
        is LivingEntity -> arg
        else -> null
    }
}
