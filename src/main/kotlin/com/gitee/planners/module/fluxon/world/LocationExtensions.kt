package com.gitee.planners.module.fluxon.world

import com.gitee.planners.api.job.target.TargetBukkitLocation
import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.Location
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type

/**
 * Location 扩展函数注册
 */
object LocationExtensions {

    fun register() {
        val runtime = FluxonScriptCache.runtime

        // TargetBukkitLocation 扩展
        runtime.registerExtension(TargetBukkitLocation::class.java)
            .function("x", FunctionSignature.returns(Type.D).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnDouble(target.getX())
            }
            .function("y", FunctionSignature.returns(Type.D).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnDouble(target.getY())
            }
            .function("z", FunctionSignature.returns(Type.D).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnDouble(target.getZ())
            }
            .function("world", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnRef(target.getBukkitWorld())
            }
            .function("location", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnRef(target.getBukkitLocation())
            }

        // Location 扩展
        runtime.registerExtension(Location::class.java)
            .function("x", FunctionSignature.returns(Type.D).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnDouble(target.x)
            }
            .function("y", FunctionSignature.returns(Type.D).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnDouble(target.y)
            }
            .function("z", FunctionSignature.returns(Type.D).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnDouble(target.z)
            }
            .function("yaw", FunctionSignature.returns(Type.F).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnFloat(target.yaw)
            }
            .function("pitch", FunctionSignature.returns(Type.F).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnFloat(target.pitch)
            }
            .function("world", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnRef(target.world)
            }
            .function("block", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnRef(target.block)
            }
            .function("clone", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
                val target = ctx.target ?: return@function
                ctx.setReturnRef(target.clone())
            }
            .function("add", FunctionSignature.returns(Type.OBJECT).params(Type.D, Type.D, Type.D)) { ctx ->
                val target = ctx.target ?: return@function
                val x = ctx.getAsDouble(0)
                val y = ctx.getAsDouble(1)
                val z = ctx.getAsDouble(2)
                ctx.setReturnRef(target.clone().add(x, y, z))
            }
            .function("distance", FunctionSignature.returns(Type.D).params(Type.OBJECT)) { ctx ->
                val target = ctx.target ?: return@function
                val other = ctx.getRef(0) as? Location
                ctx.setReturnDouble(other?.let { target.distance(it) } ?: 0.0)
            }
    }
}
