package com.gitee.planners.module.fluxon.dragoncore

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache

import com.gitee.planners.module.fluxon.getTargetsArg
import eos.moe.dragoncore.api.CoreAPI
import eos.moe.dragoncore.network.PacketSender
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.Requires
import taboolib.common.platform.Awake
import taboolib.platform.util.onlinePlayers
import java.util.*

/**
 * DragonCore 集成扩展
 */
@Requires(classes = ["eos.moe.dragoncore.DragonCore"])
object DragonCoreExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // dcParticle(scheme) - 播放粒子特效
        runtime.registerFunction("dcParticle", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val scheme = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            playParticle(scheme, 0.0, 0.0, 0.0, 100, targets)
            null
        }

        // dcParticle(scheme, x, y, z, tile) - 播放粒子特效，带偏移和时长
        runtime.registerFunction("dcParticle", returns(Type.VOID).params(Type.STRING, Type.NUMBER, Type.NUMBER, Type.NUMBER, Type.NUMBER)) { ctx ->
            val scheme = ctx.getString(0) ?: return@registerFunction
            val x = ctx.getAsDouble(1)
            val y = ctx.getAsDouble(2)
            val z = ctx.getAsDouble(3)
            val tile = ctx.getAsInt(4)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            playParticle(scheme, x, y, z, tile, targets)
            null
        }

        // dcParticle(scheme, x, y, z, tile, targets) - 播放粒子特效给目标
        runtime.registerFunction("dcParticle", returns(Type.VOID).params(Type.STRING, Type.NUMBER, Type.NUMBER, Type.NUMBER, Type.NUMBER, Type.OBJECT)) { ctx ->
            val scheme = ctx.getString(0) ?: return@registerFunction
            val x = ctx.getAsDouble(1)
            val y = ctx.getAsDouble(2)
            val z = ctx.getAsDouble(3)
            val tile = ctx.getAsInt(4)
            val targets = ctx.getTargetsArg(5, LeastType.SENDER)
            playParticle(scheme, x, y, z, tile, targets)
            null
        }

        // dcSound(name) - 播放声音
        runtime.registerFunction("dcSound", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            playSound(name, UUID.randomUUID().toString(), "music", 1f, 1f, false, targets)
            null
        }

        // dcSound(name, id, type, volume, pitch, loop) - 播放声音，全参数
        runtime.registerFunction("dcSound", returns(Type.VOID).params(Type.STRING, Type.STRING, Type.STRING, Type.NUMBER, Type.NUMBER, Type.BOOLEAN)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val id = ctx.getString(1) ?: UUID.randomUUID().toString()
            val type = ctx.getString(2) ?: "music"
            val volume = ctx.getAsDouble(3).toFloat()
            val pitch = ctx.getAsDouble(4).toFloat()
            val loop = ctx.getBool(5)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            playSound(name, id, type, volume, pitch, loop, targets)
            null
        }

        // dcSound(name, id, type, volume, pitch, loop, targets) - 播放声音给目标
        runtime.registerFunction("dcSound", returns(Type.VOID).params(Type.STRING, Type.STRING, Type.STRING, Type.NUMBER, Type.NUMBER, Type.BOOLEAN, Type.OBJECT)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val id = ctx.getString(1) ?: UUID.randomUUID().toString()
            val type = ctx.getString(2) ?: "music"
            val volume = ctx.getAsDouble(3).toFloat()
            val pitch = ctx.getAsDouble(4).toFloat()
            val loop = ctx.getBool(5)
            val targets = ctx.getTargetsArg(6, LeastType.SENDER)
            playSound(name, id, type, volume, pitch, loop, targets)
            null
        }

        // dcAnimation(name) - 播放实体动画
        runtime.registerFunction("dcAnimation", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            setAnimation(name, 0, targets)
            null
        }

        // dcAnimation(name, transition) - 播放实体动画，带过渡
        runtime.registerFunction("dcAnimation", returns(Type.VOID).params(Type.STRING, Type.NUMBER)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val transition = ctx.getAsInt(1)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            setAnimation(name, transition, targets)
            null
        }

        // dcAnimation(name, transition, targets) - 播放目标动画
        runtime.registerFunction("dcAnimation", returns(Type.VOID).params(Type.STRING, Type.NUMBER, Type.OBJECT)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val transition = ctx.getAsInt(1)
            val targets = ctx.getTargetsArg(2, LeastType.SENDER)
            setAnimation(name, transition, targets)
            null
        }

        // dcAnimationRemove(name) - 移除实体动画
        runtime.registerFunction("dcAnimationRemove", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            removeAnimation(name, 0, targets)
            null
        }

        // dcAnimationRemove(name, transition) - 移除实体动画，带过渡
        runtime.registerFunction("dcAnimationRemove", returns(Type.VOID).params(Type.STRING, Type.NUMBER)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val transition = ctx.getAsInt(1)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            removeAnimation(name, transition, targets)
            null
        }

        // dcAnimationRemove(name, transition, targets) - 移除目标动画
        runtime.registerFunction("dcAnimationRemove", returns(Type.VOID).params(Type.STRING, Type.NUMBER, Type.OBJECT)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val transition = ctx.getAsInt(1)
            val targets = ctx.getTargetsArg(2, LeastType.SENDER)
            removeAnimation(name, transition, targets)
            null
        }

        // dcPlayerAnimation(name) - 播放玩家动画
        runtime.registerFunction("dcPlayerAnimation", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                PacketSender.setPlayerAnimation(player, name)
            }
            null
        }

        // dcPlayerAnimation(name, targets) - 播放目标玩家动画
        runtime.registerFunction("dcPlayerAnimation", returns(Type.VOID).params(Type.STRING, Type.OBJECT)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                PacketSender.setPlayerAnimation(player, name)
            }
            null
        }

        // dcPlayerAnimationRemove() - 移除玩家动画
        runtime.registerFunction("dcPlayerAnimationRemove", returns(Type.VOID).noParams()) { ctx ->
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                PacketSender.removePlayerAnimation(player)
            }
            null
        }

        // dcPlayerAnimationRemove(targets) - 移除目标玩家动画
        runtime.registerFunction("dcPlayerAnimationRemove", returns(Type.VOID).params(Type.OBJECT)) { ctx ->
            val targets = ctx.getTargetsArg(0, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                PacketSender.removePlayerAnimation(player)
            }
            null
        }

        // dcSync(data) - 同步占位符
        runtime.registerFunction("dcSync", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val data = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            syncPlaceholder(data, targets)
            null
        }

        // dcSync(data, targets) - 同步占位符给目标
        runtime.registerFunction("dcSync", returns(Type.VOID).params(Type.STRING, Type.OBJECT)) { ctx ->
            val data = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            syncPlaceholder(data, targets)
            null
        }

        // dcSyncDelete(name) - 删除占位符缓存
        runtime.registerFunction("dcSyncDelete", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            deletePlaceholderCache(name, false, targets)
            null
        }

        // dcSyncDelete(name, isStartWith) - 删除占位符缓存，可选前缀匹配
        runtime.registerFunction("dcSyncDelete", returns(Type.VOID).params(Type.STRING, Type.BOOLEAN)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val isStartWith = ctx.getBool(1)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            deletePlaceholderCache(name, isStartWith, targets)
            null
        }

        // dcSyncDelete(name, isStartWith, targets) - 删除目标占位符缓存
        runtime.registerFunction("dcSyncDelete", returns(Type.VOID).params(Type.STRING, Type.BOOLEAN, Type.OBJECT)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val isStartWith = ctx.getBool(1)
            val targets = ctx.getTargetsArg(2, LeastType.SENDER)
            deletePlaceholderCache(name, isStartWith, targets)
            null
        }

        // dcEntityFunction(function) - 执行实体函数
        runtime.registerFunction("dcEntityFunction", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val function = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            executeEntityFunction(function, targets)
            null
        }

        // dcEntityFunction(function, targets) - 执行目标实体函数
        runtime.registerFunction("dcEntityFunction", returns(Type.VOID).params(Type.STRING, Type.OBJECT)) { ctx ->
            val function = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            executeEntityFunction(function, targets)
            null
        }
    }

    /**
     * 播放 DragonCore 粒子特效
     * @param scheme 粒子方案名称
     * @param x X 偏移量
     * @param y Y 偏移量
     * @param z Z 偏移量
     * @param tile 持续时间
     * @param targets 目标容器（支持实体和位置）
     */
    private fun playParticle(scheme: String, x: Double, y: Double, z: Double, tile: Int, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        targets.forEach { target ->
            val id = UUID.randomUUID().toString()
            val posOrEntityId = when (target) {
                is ProxyTarget.BukkitEntity -> target.instance.uniqueId.toString()
                is ProxyTarget.Location<*> -> "${target.getWorld()},${target.getX()},${target.getY()},${target.getZ()}"
                else -> return@forEach
            }
            onlinePlayers.forEach { player ->
                PacketSender.addParticle(player, scheme, id, posOrEntityId, "$x,$y,$z", tile)
            }
        }
    }

    /**
     * 播放 DragonCore 声音
     * @param name 声音名称
     * @param id 声音 ID
     * @param type 声音类型
     * @param volume 音量
     * @param pitch 音调
     * @param loop 是否循环
     * @param targets 目标玩家容器
     */
    private fun playSound(name: String, id: String, type: String, volume: Float, pitch: Float, loop: Boolean, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val player = target.instance as? Player ?: return@forEach
            PacketSender.sendPlaySound(player, name, id, type, volume, pitch, loop, 0f, 0f, 0f)
        }
    }

    /**
     * 播放 DragonCore 实体动画
     * @param name 动画名称
     * @param transition 过渡时间
     * @param targets 目标实体容器（仅 LivingEntity 生效）
     */
    private fun setAnimation(name: String, transition: Int, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val entity = target.instance as? LivingEntity ?: return@forEach
            CoreAPI.setEntityAnimation(entity, name, transition)
        }
    }

    /**
     * 移除 DragonCore 实体动画
     * @param name 动画名称
     * @param transition 过渡时间
     * @param targets 目标实体容器
     */
    private fun removeAnimation(name: String, transition: Int, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val entity = target.instance as? LivingEntity ?: return@forEach
            CoreAPI.removeEntityAnimation(entity, name, transition)
        }
    }

    /**
     * 同步 DragonCore 占位符到客户端
     * @param data 占位符数据（空格分隔的键值对，键值用逗号分隔）
     * @param targets 目标玩家容器
     */
    private fun syncPlaceholder(data: String, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        val map = data.split(" ").associate {
            val parts = it.split(",")
            parts[0] to parts.getOrElse(1) { "" }
        }
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val player = target.instance as? Player ?: return@forEach
            PacketSender.sendSyncPlaceholder(player, map)
        }
    }

    /**
     * 删除 DragonCore 占位符缓存
     * @param name 占位符名称
     * @param isStartWith 是否使用前缀匹配
     * @param targets 目标玩家容器
     */
    private fun deletePlaceholderCache(name: String, isStartWith: Boolean, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val player = target.instance as? Player ?: return@forEach
            PacketSender.sendDeletePlaceholderCache(player, name, isStartWith)
        }
    }

    /**
     * 执行 DragonCore 实体动画函数
     * @param function 函数名称
     * @param targets 目标实体容器
     */
    private fun executeEntityFunction(function: String, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val entity = target.instance as? LivingEntity ?: return@forEach
            onlinePlayers.forEach { player ->
                PacketSender.runEntityAnimationFunction(player, entity.uniqueId, function)
            }
        }
    }
}
