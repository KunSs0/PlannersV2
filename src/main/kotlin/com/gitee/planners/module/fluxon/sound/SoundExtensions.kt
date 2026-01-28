package com.gitee.planners.module.fluxon.sound

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache

import com.gitee.planners.module.fluxon.getTargetsArg
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 声音播放扩展
 */
object SoundExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // sound(name) - 播放原版声音给 sender
        runtime.registerFunction("sound", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            playSound(name, 1.0f, 1.0f, targets)
            null
        }

        // sound(name, volume) - 播放原版声音，指定音量
        runtime.registerFunction("sound", returns(Type.VOID).params(Type.STRING, Type.NUMBER)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val volume = ctx.getAsDouble(1).toFloat()
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            playSound(name, volume, 1.0f, targets)
            null
        }

        // sound(name, volume, pitch) - 播放原版声音，指定音量和音调
        runtime.registerFunction("sound", returns(Type.VOID).params(Type.STRING, Type.NUMBER, Type.NUMBER)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val volume = ctx.getAsDouble(1).toFloat()
            val pitch = ctx.getAsDouble(2).toFloat()
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            playSound(name, volume, pitch, targets)
            null
        }

        // sound(name, volume, pitch, targets) - 播放原版声音给目标
        runtime.registerFunction("sound", returns(Type.VOID).params(Type.STRING, Type.NUMBER, Type.NUMBER, Type.OBJECT)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val volume = ctx.getAsDouble(1).toFloat()
            val pitch = ctx.getAsDouble(2).toFloat()
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)
            playSound(name, volume, pitch, targets)
            null
        }

        // soundResource(name) - 播放资源包声音给 sender
        runtime.registerFunction("soundResource", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            playResourceSound(name, 1.0f, 1.0f, targets)
            null
        }

        // soundResource(name, volume) - 播放资源包声音，指定音量
        runtime.registerFunction("soundResource", returns(Type.VOID).params(Type.STRING, Type.NUMBER)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val volume = ctx.getAsDouble(1).toFloat()
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            playResourceSound(name, volume, 1.0f, targets)
            null
        }

        // soundResource(name, volume, pitch) - 播放资源包声音，指定音量和音调
        runtime.registerFunction("soundResource", returns(Type.VOID).params(Type.STRING, Type.NUMBER, Type.NUMBER)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val volume = ctx.getAsDouble(1).toFloat()
            val pitch = ctx.getAsDouble(2).toFloat()
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            playResourceSound(name, volume, pitch, targets)
            null
        }

        // soundResource(name, volume, pitch, targets) - 播放资源包声音给目标
        runtime.registerFunction("soundResource", returns(Type.VOID).params(Type.STRING, Type.NUMBER, Type.NUMBER, Type.OBJECT)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val volume = ctx.getAsDouble(1).toFloat()
            val pitch = ctx.getAsDouble(2).toFloat()
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)
            playResourceSound(name, volume, pitch, targets)
            null
        }
    }

    private fun playSound(name: String, volume: Float, pitch: Float, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        val sound = runCatching { Sound.valueOf(name.uppercase()) }.getOrNull() ?: return
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val player = target.instance as? Player ?: return@forEach
            player.playSound(player.location, sound, volume, pitch)
        }
    }

    private fun playResourceSound(name: String, volume: Float, pitch: Float, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val player = target.instance as? Player ?: return@forEach
            player.playSound(player.location, name, volume, pitch)
        }
    }
}
