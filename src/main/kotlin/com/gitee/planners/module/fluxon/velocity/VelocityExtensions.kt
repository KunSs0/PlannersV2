package com.gitee.planners.module.fluxon.velocity

import com.gitee.planners.module.fluxon.FluxonFunctionContext
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.registerFunction
import org.bukkit.entity.Entity
import org.bukkit.util.Vector
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 实体速度控制扩展
 */
object VelocityExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // setVelocity(x, y, z, [entity]) -> void
        runtime.registerFunction("setVelocity", listOf(3, 4)) { ctx ->
            val x = (ctx.arguments[0] as Number).toDouble()
            val y = (ctx.arguments[1] as Number).toDouble()
            val z = (ctx.arguments[2] as Number).toDouble()
            ctx.getEntityArg(3).velocity = Vector(x, y, z)
            null
        }

        // addVelocity(x, y, z, [entity]) -> void
        runtime.registerFunction("addVelocity", listOf(3, 4)) { ctx ->
            val x = (ctx.arguments[0] as Number).toDouble()
            val y = (ctx.arguments[1] as Number).toDouble()
            val z = (ctx.arguments[2] as Number).toDouble()
            val entity = ctx.getEntityArg(3)
            entity.velocity = entity.velocity.add(Vector(x, y, z))
            null
        }

        // getVelocity([entity]) -> Vector
        runtime.registerFunction("getVelocity", listOf(0, 1)) { ctx ->
            ctx.getEntityArg(0).velocity
        }
    }

    private fun FluxonFunctionContext.getEntityArg(index: Int): Entity {
        if (arguments.size > index) {
            return arguments[index] as? Entity
                ?: throw IllegalStateException("Argument at $index is not an entity")
        }
        return (environment.rootVariables["target"] ?: environment.rootVariables["player"]) as? Entity
            ?: throw IllegalStateException("No entity found in environment")
    }
}
