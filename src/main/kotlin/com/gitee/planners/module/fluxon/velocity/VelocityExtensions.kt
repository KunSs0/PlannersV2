package com.gitee.planners.module.fluxon.velocity

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
    fun init() {
        val runtime = FluxonScriptCache.runtime

        runtime.registerFunction("setVelocity", listOf(4)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val entity = ctx.getRef(3) as? Entity ?: return@registerFunction null
            entity.velocity = Vector(x, y, z)
            null
        }

        runtime.registerFunction("addVelocity", listOf(4)) { ctx ->
            val x = ctx.getAsDouble(0)
            val y = ctx.getAsDouble(1)
            val z = ctx.getAsDouble(2)
            val entity = ctx.getRef(3) as? Entity ?: return@registerFunction null
            entity.velocity = entity.velocity.add(Vector(x, y, z))
            null
        }

        runtime.registerFunction("getVelocity", listOf(1)) { ctx ->
            val entity = ctx.getRef(0) as? Entity ?: return@registerFunction Vector()
            entity.velocity
        }
    }
}
