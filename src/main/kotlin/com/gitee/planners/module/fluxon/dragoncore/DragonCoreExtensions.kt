package com.gitee.planners.module.fluxon.dragoncore

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.getTargetsArg
import com.gitee.planners.module.fluxon.registerFunction
import eos.moe.dragoncore.api.CoreAPI
import eos.moe.dragoncore.network.PacketSender
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
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

        // dcParticle(scheme, [x], [y], [z], [tile], [targets]) - 播放粒子特效
        runtime.registerFunction("dcParticle", listOf(1, 4, 5, 6)) { ctx ->
            val scheme = ctx.getAsString(0) ?: return@registerFunction null
            val x = if (ctx.arguments.size > 1) ctx.getAsDouble(1) else 0.0
            val y = if (ctx.arguments.size > 2) ctx.getAsDouble(2) else 0.0
            val z = if (ctx.arguments.size > 3) ctx.getAsDouble(3) else 0.0
            val tile = if (ctx.arguments.size > 4) ctx.getAsInt(4) else 100
            val targets = ctx.getTargetsArg(5, LeastType.SENDER)

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
            null
        }

        // dcSound(name, [id], [type], [volume], [pitch], [loop], [targets]) - 播放声音
        runtime.registerFunction("dcSound", listOf(1, 2, 3, 4, 5, 6, 7)) { ctx ->
            val name = ctx.getAsString(0) ?: return@registerFunction null
            val id = if (ctx.arguments.size > 1) ctx.getAsString(1) ?: UUID.randomUUID().toString() else UUID.randomUUID().toString()
            val type = if (ctx.arguments.size > 2) ctx.getAsString(2) ?: "music" else "music"
            val volume = if (ctx.arguments.size > 3) ctx.getAsDouble(3).toFloat() else 1f
            val pitch = if (ctx.arguments.size > 4) ctx.getAsDouble(4).toFloat() else 1f
            val loop = if (ctx.arguments.size > 5) ctx.getRef(5) as? Boolean ?: false else false
            val targets = ctx.getTargetsArg(6, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                PacketSender.sendPlaySound(player, name, id, type, volume, pitch, loop, 0f, 0f, 0f)
            }
            null
        }

        // dcAnimation(name, [transition], [targets]) - 播放实体动画
        runtime.registerFunction("dcAnimation", listOf(1, 2, 3)) { ctx ->
            val name = ctx.getAsString(0) ?: return@registerFunction null
            val transition = if (ctx.arguments.size > 1) ctx.getAsInt(1) else 0
            val targets = ctx.getTargetsArg(2, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val entity = target.instance as? LivingEntity ?: return@forEach
                CoreAPI.setEntityAnimation(entity, name, transition)
            }
            null
        }

        // dcAnimationRemove(name, [transition], [targets]) - 移除实体动画
        runtime.registerFunction("dcAnimationRemove", listOf(1, 2, 3)) { ctx ->
            val name = ctx.getAsString(0) ?: return@registerFunction null
            val transition = if (ctx.arguments.size > 1) ctx.getAsInt(1) else 0
            val targets = ctx.getTargetsArg(2, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val entity = target.instance as? LivingEntity ?: return@forEach
                CoreAPI.removeEntityAnimation(entity, name, transition)
            }
            null
        }

        // dcPlayerAnimation(name, [targets]) - 播放玩家动画
        runtime.registerFunction("dcPlayerAnimation", listOf(1, 2)) { ctx ->
            val name = ctx.getAsString(0) ?: return@registerFunction null
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                PacketSender.setPlayerAnimation(player, name)
            }
            null
        }

        // dcPlayerAnimationRemove([targets]) - 移除玩家动画
        runtime.registerFunction("dcPlayerAnimationRemove", listOf(0, 1)) { ctx ->
            val targets = ctx.getTargetsArg(0, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                PacketSender.removePlayerAnimation(player)
            }
            null
        }

        // dcSync(data, [targets]) - 同步占位符
        runtime.registerFunction("dcSync", listOf(1, 2)) { ctx ->
            val data = ctx.getAsString(0) ?: return@registerFunction null
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)

            val map = data.split(" ").associate {
                val parts = it.split(",")
                parts[0] to parts.getOrElse(1) { "" }
            }

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                PacketSender.sendSyncPlaceholder(player, map)
            }
            null
        }

        // dcSyncDelete(name, [isStartWith], [targets]) - 删除占位符缓存
        runtime.registerFunction("dcSyncDelete", listOf(1, 2, 3)) { ctx ->
            val name = ctx.getAsString(0) ?: return@registerFunction null
            val isStartWith = if (ctx.arguments.size > 1) ctx.getRef(1) as? Boolean ?: false else false
            val targets = ctx.getTargetsArg(2, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                PacketSender.sendDeletePlaceholderCache(player, name, isStartWith)
            }
            null
        }

        // dcEntityFunction(function, [targets]) - 执行实体函数
        runtime.registerFunction("dcEntityFunction", listOf(1, 2)) { ctx ->
            val function = ctx.getAsString(0) ?: return@registerFunction null
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val entity = target.instance as? LivingEntity ?: return@forEach
                onlinePlayers.forEach { player ->
                    PacketSender.runEntityAnimationFunction(player, entity.uniqueId, function)
                }
            }
            null
        }
    }
}
