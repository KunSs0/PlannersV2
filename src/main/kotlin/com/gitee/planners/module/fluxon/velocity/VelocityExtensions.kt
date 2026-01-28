package com.gitee.planners.module.fluxon.velocity

import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.entity.Entity
import org.bukkit.util.Vector
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import org.tabooproject.fluxon.runtime.java.Export
import org.tabooproject.fluxon.runtime.java.Optional
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 实体速度控制扩展
 */
object VelocityExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime
        runtime.registerFunction("pl:velocity", "velocity", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
            ctx.setReturnRef(VelocityObject)
        }
        runtime.exportRegistry.registerClass(VelocityObject::class.java, "pl:velocity")
    }

    object VelocityObject {

        @JvmField
        val TYPE: Type = Type.fromClass(VelocityObject::class.java)

        @Export
        fun set(x: Double, y: Double, z: Double, @Optional entity: Entity) {
            entity.velocity = Vector(x, y, z)
        }

        @Export
        fun add(x: Double, y: Double, z: Double, @Optional entity: Entity) {
            entity.velocity = entity.velocity.add(Vector(x, y, z))
        }

        @Export
        fun get(@Optional entity: Entity): Vector {
            return entity.velocity
        }
    }
}
