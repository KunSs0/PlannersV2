package com.gitee.planners.module.fluxon.common

import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type

/**
 * 通用扩展函数注册
 */
object CommonExtensions {

    fun register() {
        val runtime = FluxonScriptCache.runtime

        // Player 扩展 - 播放声音
        runtime.registerExtension(Player::class.java)
            .function("playSound", FunctionSignature.returns(Type.VOID).params(Type.OBJECT)) { ctx ->
                val player = ctx.target ?: return@function
                val soundName = ctx.getRef(0)?.toString() ?: return@function
                try {
                    val sound = Sound.valueOf(soundName.uppercase().replace(".", "_"))
                    player.playSound(player.location, sound, 1f, 1f)
                } catch (e: Exception) {
                    player.playSound(player.location, soundName, 1f, 1f)
                }
            }
            .function("playSound", FunctionSignature.returns(Type.VOID).params(Type.OBJECT, Type.F, Type.F)) { ctx ->
                val player = ctx.target ?: return@function
                val soundName = ctx.getRef(0)?.toString() ?: return@function
                val volume = ctx.getFloat(1)
                val pitch = ctx.getFloat(2)
                try {
                    val sound = Sound.valueOf(soundName.uppercase().replace(".", "_"))
                    player.playSound(player.location, sound, volume, pitch)
                } catch (e: Exception) {
                    player.playSound(player.location, soundName, volume, pitch)
                }
            }

        // Location 扩展 - 播放声音
        runtime.registerExtension(Location::class.java)
            .function("playSound", FunctionSignature.returns(Type.VOID).params(Type.OBJECT, Type.F, Type.F)) { ctx ->
                val location = ctx.target ?: return@function
                val soundName = ctx.getRef(0)?.toString() ?: return@function
                val volume = ctx.getFloat(1)
                val pitch = ctx.getFloat(2)
                try {
                    val sound = Sound.valueOf(soundName.uppercase().replace(".", "_"))
                    location.world?.playSound(location, sound, volume, pitch)
                } catch (e: Exception) {
                    location.world?.playSound(location, soundName, volume, pitch)
                }
            }
    }
}
