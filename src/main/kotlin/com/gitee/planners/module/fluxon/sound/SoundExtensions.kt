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

        /**
         * 播放原版声音给 sender（默认音量和音调）
         * @param name 原版声音名称（如 ENTITY_PLAYER_LEVELUP）
         */
        runtime.registerFunction("sound", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            playSound(name, 1.0f, 1.0f, targets)
            null
        }

        /**
         * 播放原版声音，指定音量
         * @param name 原版声音名称
         * @param volume 音量（1.0=正常音量，可超过 1.0 增大传播距离）
         */
        runtime.registerFunction("sound", returns(Type.VOID).params(Type.STRING, Type.F)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val volume = ctx.getFloat(1)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            playSound(name, volume, 1.0f, targets)
            null
        }

        /**
         * 播放原版声音，指定音量和音调
         * @param name 原版声音名称
         * @param volume 音量
         * @param pitch 音调（0.5-2.0，1.0=正常音调）
         */
        runtime.registerFunction("sound", returns(Type.VOID).params(Type.STRING, Type.F, Type.F)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val volume = ctx.getFloat(1)
            val pitch = ctx.getFloat(2)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            playSound(name, volume, pitch, targets)
            null
        }

        /**
         * 播放原版声音给指定目标
         * @param name 原版声音名称
         * @param volume 音量
         * @param pitch 音调
         * @param targets 目标玩家
         */
        runtime.registerFunction("sound", returns(Type.VOID).params(Type.STRING, Type.F, Type.F, Type.OBJECT)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val volume = ctx.getFloat(1)
            val pitch = ctx.getFloat(2)
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)
            playSound(name, volume, pitch, targets)
            null
        }

        /**
         * 播放资源包自定义声音给 sender
         * @param name 资源包中定义的声音 ID（如 custom.skill.fire）
         */
        runtime.registerFunction("soundResource", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            playResourceSound(name, 1.0f, 1.0f, targets)
            null
        }

        /**
         * 播放资源包声音，指定音量
         * @param name 声音 ID
         * @param volume 音量
         */
        runtime.registerFunction("soundResource", returns(Type.VOID).params(Type.STRING, Type.F)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val volume = ctx.getFloat(1)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            playResourceSound(name, volume, 1.0f, targets)
            null
        }

        /**
         * 播放资源包声音，指定音量和音调
         * @param name 声音 ID
         * @param volume 音量
         * @param pitch 音调
         */
        runtime.registerFunction("soundResource", returns(Type.VOID).params(Type.STRING, Type.F, Type.F)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val volume = ctx.getFloat(1)
            val pitch = ctx.getFloat(2)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            playResourceSound(name, volume, pitch, targets)
            null
        }

        /**
         * 播放资源包声音给指定目标
         * @param name 声音 ID
         * @param volume 音量
         * @param pitch 音调
         * @param targets 目标玩家
         */
        runtime.registerFunction("soundResource", returns(Type.VOID).params(Type.STRING, Type.F, Type.F, Type.OBJECT)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val volume = ctx.getFloat(1)
            val pitch = ctx.getFloat(2)
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)
            playResourceSound(name, volume, pitch, targets)
            null
        }
    }

    /**
     * 播放原版声音
     * @param name 声音枚举名称（大写）
     * @param volume 音量
     * @param pitch 音调
     * @param targets 目标玩家容器
     */
    private fun playSound(name: String, volume: Float, pitch: Float, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        val sound = runCatching { Sound.valueOf(name.uppercase()) }.getOrNull() ?: return
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val player = target.instance as? Player ?: return@forEach
            player.playSound(player.location, sound, volume, pitch)
        }
    }

    /**
     * 播放资源包自定义声音
     * @param name 声音 ID（如 custom.skill.fire）
     * @param volume 音量
     * @param pitch 音调
     * @param targets 目标玩家容器
     */
    private fun playResourceSound(name: String, volume: Float, pitch: Float, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val player = target.instance as? Player ?: return@forEach
            player.playSound(player.location, name, volume, pitch)
        }
    }
}
