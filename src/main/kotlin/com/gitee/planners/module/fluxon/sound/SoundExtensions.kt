package com.gitee.planners.module.fluxon.sound

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.getTargetsArg
import com.gitee.planners.module.fluxon.registerFunction
import org.bukkit.Sound
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 声音播放扩展
 */
object SoundExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // sound(name, [volume], [pitch], [targets]) - 播放原版声音
        runtime.registerFunction("sound", listOf(1, 2, 3, 4)) { ctx ->
            val name = ctx.getAsString(0) ?: return@registerFunction null
            val volume = if (ctx.arguments.size > 1) ctx.getAsDouble(1).toFloat() else 1.0f
            val pitch = if (ctx.arguments.size > 2) ctx.getAsDouble(2).toFloat() else 1.0f
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)

            val sound = runCatching { Sound.valueOf(name.uppercase()) }.getOrNull()
                ?: return@registerFunction null

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                player.playSound(player.location, sound, volume, pitch)
            }
            null
        }

        // soundResource(name, [volume], [pitch], [targets]) - 播放资源包声音
        runtime.registerFunction("soundResource", listOf(1, 2, 3, 4)) { ctx ->
            val name = ctx.getAsString(0) ?: return@registerFunction null
            val volume = if (ctx.arguments.size > 1) ctx.getAsDouble(1).toFloat() else 1.0f
            val pitch = if (ctx.arguments.size > 2) ctx.getAsDouble(2).toFloat() else 1.0f
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                player.playSound(player.location, name, volume, pitch)
            }
            null
        }
    }
}
