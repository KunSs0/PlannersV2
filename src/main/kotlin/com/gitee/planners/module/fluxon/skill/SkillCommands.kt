package com.gitee.planners.module.fluxon.skill

import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.entity.LivingEntity
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import taboolib.platform.util.setMeta

/**
 * 技能相关扩展函数注册
 */
object SkillCommands {

    fun register() {
        val runtime = FluxonScriptCache.runtime

        // ProxyTarget.BukkitEntity 扩展 - damage
        runtime.registerExtension(ProxyTarget.BukkitEntity::class.java)
            .function("damage", FunctionSignature.returns(Type.VOID).params(Type.D)) { ctx ->
                val target = ctx.target ?: return@function
                val entity = target.instance as? LivingEntity ?: return@function
                entity.damage(ctx.getAsDouble(0))
            }
            .function("damage", FunctionSignature.returns(Type.VOID).params(Type.D, Type.OBJECT)) { ctx ->
                val target = ctx.target ?: return@function
                val entity = target.instance as? LivingEntity ?: return@function
                val amount = ctx.getAsDouble(0)
                val sourceArg = ctx.getRef(1)
                val killer = when (sourceArg) {
                    is ProxyTarget.BukkitEntity -> sourceArg.instance as? LivingEntity
                    is LivingEntity -> sourceArg
                    else -> null
                }
                if (killer != null && killer != entity) {
                    if (entity.health <= amount) {
                        entity.setMeta("@killer", killer)
                    }
                }
                entity.damage(amount)
            }
            .function("heal", FunctionSignature.returns(Type.VOID).params(Type.D)) { ctx ->
                val target = ctx.target ?: return@function
                val entity = target.instance as? LivingEntity ?: return@function
                val amount = ctx.getAsDouble(0)
                entity.health = (entity.health + amount).coerceAtMost(entity.maxHealth)
            }

        // LivingEntity 扩展 - damage/heal
        runtime.registerExtension(LivingEntity::class.java)
            .function("damage", FunctionSignature.returns(Type.VOID).params(Type.D)) { ctx ->
                val target = ctx.target ?: return@function
                target.damage(ctx.getAsDouble(0))
            }
            .function("heal", FunctionSignature.returns(Type.VOID).params(Type.D)) { ctx ->
                val target = ctx.target ?: return@function
                val amount = ctx.getAsDouble(0)
                target.health = (target.health + amount).coerceAtMost(target.maxHealth)
            }
    }
}
