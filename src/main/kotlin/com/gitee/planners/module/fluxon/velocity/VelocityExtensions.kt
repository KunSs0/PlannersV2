package com.gitee.planners.module.fluxon.velocity

import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.entity.Entity
import org.bukkit.util.Vector
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type

/**
 * 实体速度控制扩展
 */
object VelocityExtensions {

    fun register() {
        val runtime = FluxonScriptCache.runtime

        // Entity 速度扩展
        runtime.registerExtension(Entity::class.java)
            .function("setVelocity", FunctionSignature.returns(Type.VOID).params(Type.D, Type.D, Type.D)) { ctx ->
                val entity = ctx.target ?: return@function
                val x = ctx.getAsDouble(0)
                val y = ctx.getAsDouble(1)
                val z = ctx.getAsDouble(2)
                entity.velocity = Vector(x, y, z)
            }
            .function("addVelocity", FunctionSignature.returns(Type.VOID).params(Type.D, Type.D, Type.D)) { ctx ->
                val entity = ctx.target ?: return@function
                val x = ctx.getAsDouble(0)
                val y = ctx.getAsDouble(1)
                val z = ctx.getAsDouble(2)
                entity.velocity = entity.velocity.add(Vector(x, y, z))
            }
            .function("getVelocity", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
                val entity = ctx.target ?: return@function
                ctx.setReturnRef(entity.velocity)
            }
    }
}
