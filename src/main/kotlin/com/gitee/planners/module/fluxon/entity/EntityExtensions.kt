package com.gitee.planners.module.fluxon.entity

import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type

/**
 * Entity 扩展函数注册
 */
object EntityExtensions {

    fun register() {
        val runtime = FluxonScriptCache.runtime

        // TargetBukkitEntity 扩展
        runtime.registerExtension(TargetBukkitEntity::class.java)
            .function("id", FunctionSignature.returns(Type.I).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnInt(target.instance.entityId)
            }
            .function("name", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnRef(target.instance.name)
            }
            .function("location", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnRef(target.instance.location)
            }
            .function("world", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnRef(target.instance.world)
            }
            .function("uuid", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnRef(target.instance.uniqueId)
            }
            .function("health", FunctionSignature.returns(Type.D).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                val health = (target.instance as? LivingEntity)?.health ?: 0.0
                ctx.setReturnDouble(health)
            }
            .function("maxHealth", FunctionSignature.returns(Type.D).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                val maxHealth = (target.instance as? LivingEntity)?.maxHealth ?: 0.0
                ctx.setReturnDouble(maxHealth)
            }

        // Entity 扩展
        runtime.registerExtension(Entity::class.java)
            .function("id", FunctionSignature.returns(Type.I).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnInt(target.entityId)
            }
            .function("name", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnRef(target.name)
            }
            .function("location", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnRef(target.location)
            }
            .function("world", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnRef(target.world)
            }
            .function("uuid", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnRef(target.uniqueId)
            }

        // LivingEntity 扩展
        runtime.registerExtension(LivingEntity::class.java)
            .function("health", FunctionSignature.returns(Type.D).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnDouble(target.health)
            }
            .function("maxHealth", FunctionSignature.returns(Type.D).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnDouble(target.maxHealth)
            }
            .function("isDead", FunctionSignature.returns(Type.Z).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnBool(target.isDead)
            }
            .function("setHealth", FunctionSignature.returns(Type.VOID).params(Type.D)) { ctx ->
                val target = ctx.target ?: return@function
                target.health = ctx.getAsDouble(0)
            }
    }
}
