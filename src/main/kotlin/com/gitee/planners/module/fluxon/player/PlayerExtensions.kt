package com.gitee.planners.module.fluxon.player

import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type

/**
 * Player 扩展函数
 */
object PlayerExtensions {

    fun register() {
        val runtime = FluxonScriptCache.runtime

        // Player 扩展
        runtime.registerExtension(Player::class.java)
            .function("yaw", FunctionSignature.returns(Type.F).noParams()) { ctx ->
                val player = ctx.target ?: return@function
                ctx.setReturnFloat(player.location.yaw)
            }
            .function("pitch", FunctionSignature.returns(Type.F).noParams()) { ctx ->
                val player = ctx.target ?: return@function
                ctx.setReturnFloat(player.location.pitch)
            }
            .function("lookLocation", FunctionSignature.returns(Type.OBJECT).params(Type.D)) { ctx ->
                val player = ctx.target ?: return@function
                val distance = ctx.getAsDouble(0)
                val direction = player.location.direction.normalize()
                val targetLocation = player.eyeLocation.clone().add(direction.multiply(distance))
                ctx.setReturnRef(targetLocation)
            }
            .function("name", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
                val player = ctx.target ?: return@function
                ctx.setReturnRef(player.name)
            }
            .function("uuid", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
                val player = ctx.target ?: return@function
                ctx.setReturnRef(player.uniqueId.toString())
            }
            .function("eyeLocation", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
                val player = ctx.target ?: return@function
                ctx.setReturnRef(player.eyeLocation)
            }
            .function("teleport", FunctionSignature.returns(Type.VOID).params(Type.OBJECT)) { ctx ->
                val player = ctx.target ?: return@function
                val location = ctx.getRef(0) as? org.bukkit.Location ?: return@function
                player.teleport(location)
            }
    }
}
