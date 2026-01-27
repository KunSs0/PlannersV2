package com.gitee.planners.module.fluxon.velocity

import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.entity.Entity
import org.bukkit.util.Vector
import org.tabooproject.fluxon.runtime.FunctionContext
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 实体速度控制扩展
 */
object VelocityExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // setVelocity(x, y, z) -> void (从环境获取entity)
        runtime.registerFunction("setVelocity", FunctionSignature.returns(Type.VOID).params(Type.D, Type.D, Type.D)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val entity = getEntityFromEnv(ctx)
            entity.velocity = Vector(x, y, z)
        }

        // setVelocity(x, y, z, entity) -> void
        runtime.registerFunction("setVelocity", FunctionSignature.returns(Type.VOID).params(Type.D, Type.D, Type.D, Type.OBJECT)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val entity = ctx.getRef(3) as? Entity ?: return@registerFunction
            entity.velocity = Vector(x, y, z)
        }

        // addVelocity(x, y, z) -> void (从环境获取entity)
        runtime.registerFunction("addVelocity", FunctionSignature.returns(Type.VOID).params(Type.D, Type.D, Type.D)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val entity = getEntityFromEnv(ctx)
            entity.velocity = entity.velocity.add(Vector(x, y, z))
        }

        // addVelocity(x, y, z, entity) -> void
        runtime.registerFunction("addVelocity", FunctionSignature.returns(Type.VOID).params(Type.D, Type.D, Type.D, Type.OBJECT)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val entity = ctx.getRef(3) as? Entity ?: return@registerFunction
            entity.velocity = entity.velocity.add(Vector(x, y, z))
        }

        // getVelocity() -> Vector (从环境获取entity)
        runtime.registerFunction("getVelocity", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
            val entity = getEntityFromEnv(ctx)
            ctx.setReturnRef(entity.velocity)
        }

        // getVelocity(entity) -> Vector
        runtime.registerFunction("getVelocity", FunctionSignature.returns(Type.OBJECT).params(Type.OBJECT)) { ctx ->
            val entity = ctx.getRef(0) as? Entity ?: return@registerFunction
            ctx.setReturnRef(entity.velocity)
        }
    }

    private fun getEntityFromEnv(ctx: FunctionContext<*>): Entity {
        val find = ctx.environment.rootVariables["target"]
            ?: ctx.environment.rootVariables["player"]
        if (find is Entity) {
            return find
        }
        throw IllegalStateException("No entity found in environment")
    }
}
