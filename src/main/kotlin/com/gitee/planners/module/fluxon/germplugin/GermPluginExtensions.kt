package com.gitee.planners.module.fluxon.germplugin

import com.germ.germplugin.api.GermPacketAPI
import com.germ.germplugin.api.RootType
import com.germ.germplugin.api.SoundType
import com.germ.germplugin.api.ViewType
import com.germ.germplugin.api.bean.AnimDataDTO
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.getTargetsArg
import com.gitee.planners.module.fluxon.registerFunction
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.Requires
import taboolib.common.platform.Awake
import taboolib.platform.util.onlinePlayers
import java.util.*

/**
 * GermPlugin 集成扩展
 */
@Requires(classes = ["com.germ.germplugin.GermPlugin"])
object GermPluginExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // germEffect(name, [index], [targets]) - 播放特效
        runtime.registerFunction("germEffect", listOf(1, 2, 3)) { ctx ->
            val name = ctx.getAsString(0) ?: return@registerFunction null
            val index = if (ctx.arguments.size > 1) ctx.getAsString(1) ?: UUID.randomUUID().toString() else UUID.randomUUID().toString()
            val targets = ctx.getTargetsArg(2, LeastType.SENDER)

            targets.forEach { target ->
                when (target) {
                    is ProxyTarget.BukkitEntity -> {
                        val loc = target.getBukkitLocation()
                        onlinePlayers.forEach { player ->
                            GermPacketAPI.sendEffect(player, name, index, loc.x, loc.y, loc.z)
                        }
                    }
                    is ProxyTarget.Location<*> -> {
                        val loc = target.getBukkitLocation()
                        onlinePlayers.forEach { player ->
                            GermPacketAPI.sendEffect(player, name, index, loc.x, loc.y, loc.z)
                        }
                    }
                    else -> {}
                }
            }
            index
        }

        // germEffectRemove(index, [targets]) - 移除特效
        runtime.registerFunction("germEffectRemove", listOf(1, 2)) { ctx ->
            val index = ctx.getAsString(0) ?: return@registerFunction null
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                GermPacketAPI.removeEffect(player, index)
            }
            null
        }

        // germEffectClear([targets]) - 清除所有特效
        runtime.registerFunction("germEffectClear", listOf(0, 1)) { ctx ->
            val targets = ctx.getTargetsArg(0, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                GermPacketAPI.clearEffect(player)
            }
            null
        }

        // germSound(name, [type], [volume], [pitch], [targets]) - 播放声音
        runtime.registerFunction("germSound", listOf(1, 2, 3, 4, 5)) { ctx ->
            val name = ctx.getAsString(0) ?: return@registerFunction null
            val typeName = if (ctx.arguments.size > 1) ctx.getAsString(1) ?: "MASTER" else "MASTER"
            val volume = if (ctx.arguments.size > 2) ctx.getAsDouble(2).toFloat() else 1f
            val pitch = if (ctx.arguments.size > 3) ctx.getAsDouble(3).toFloat() else 1f
            val targets = ctx.getTargetsArg(4, LeastType.SENDER)

            val type = runCatching { SoundType.valueOf(typeName.uppercase()) }.getOrElse { SoundType.MASTER }

            targets.forEach { target ->
                when (target) {
                    is ProxyTarget.BukkitEntity -> {
                        val player = target.instance as? Player ?: return@forEach
                        val loc = player.location
                        GermPacketAPI.playSound(player, name, type, loc.x.toFloat(), loc.y.toFloat(), loc.z.toFloat(), 0, volume, pitch)
                    }
                    is ProxyTarget.Location<*> -> {
                        GermPacketAPI.playSound(target.getBukkitLocation(), name, type, 0, volume, pitch)
                    }
                    else -> {}
                }
            }
            null
        }

        // germAnimation(name, [speed], [reverse], [targets]) - 播放动画
        runtime.registerFunction("germAnimation", listOf(1, 2, 3, 4)) { ctx ->
            val name = ctx.getAsString(0) ?: return@registerFunction null
            val speed = if (ctx.arguments.size > 1) ctx.getAsDouble(1).toFloat() else 1f
            val reverse = if (ctx.arguments.size > 2) ctx.getRef(2) as? Boolean ?: false else false
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)

            val animData = AnimDataDTO(name, speed, reverse)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val entityId = target.instance.entityId
                onlinePlayers.forEach { sender ->
                    if (target.instance is Player) {
                        GermPacketAPI.sendBendAction(sender, entityId, animData)
                    } else {
                        GermPacketAPI.sendModelAnimation(sender, entityId, animData)
                    }
                }
            }
            null
        }

        // germAnimationStop(name, [targets]) - 停止动画
        runtime.registerFunction("germAnimationStop", listOf(1, 2)) { ctx ->
            val name = ctx.getAsString(0) ?: return@registerFunction null
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val entityId = target.instance.entityId
                onlinePlayers.forEach { sender ->
                    if (target.instance is Player) {
                        GermPacketAPI.sendBendClear(sender, entityId)
                    } else {
                        GermPacketAPI.stopModelAnimation(sender, entityId, name)
                    }
                }
            }
            null
        }

        // germViewLock([duration], [type], [targets]) - 锁定视角
        runtime.registerFunction("germViewLock", listOf(0, 1, 2, 3)) { ctx ->
            val duration = if (ctx.arguments.size > 0) ctx.getAsLong(0) else -1L
            val typeName = if (ctx.arguments.size > 1) ctx.getAsString(1) ?: "FIRST_PERSON" else "FIRST_PERSON"
            val targets = ctx.getTargetsArg(2, LeastType.SENDER)

            val type = runCatching { ViewType.valueOf(typeName.uppercase()) }.getOrElse { ViewType.FIRST_PERSON }

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                GermPacketAPI.sendLockPlayerCameraView(player, type, duration)
            }
            null
        }

        // germViewUnlock([targets]) - 解锁视角
        runtime.registerFunction("germViewUnlock", listOf(0, 1)) { ctx ->
            val targets = ctx.getTargetsArg(0, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                GermPacketAPI.sendUnlockPlayerCameraView(player)
            }
            null
        }

        // germLookLock([duration], [targets]) - 锁定视线
        runtime.registerFunction("germLookLock", listOf(0, 1, 2)) { ctx ->
            val duration = if (ctx.arguments.size > 0) ctx.getAsLong(0) else -1L
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                GermPacketAPI.sendLockPlayerCameraRotate(player, duration)
            }
            null
        }

        // germLookUnlock([targets]) - 解锁视线
        runtime.registerFunction("germLookUnlock", listOf(0, 1)) { ctx ->
            val targets = ctx.getTargetsArg(0, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                GermPacketAPI.sendUnlockPlayerCameraRotate(player)
            }
            null
        }

        // germMoveLock([duration], [targets]) - 锁定移动
        runtime.registerFunction("germMoveLock", listOf(0, 1, 2)) { ctx ->
            val duration = if (ctx.arguments.size > 0) ctx.getAsLong(0) else -1L
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                GermPacketAPI.sendLockPlayerMove(player, duration)
            }
            null
        }

        // germMoveUnlock([targets]) - 解锁移动
        runtime.registerFunction("germMoveUnlock", listOf(0, 1)) { ctx ->
            val targets = ctx.getTargetsArg(0, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                GermPacketAPI.sendUnlockPlayerMove(player)
            }
            null
        }

        // germCooldown(slot, tick, [targets]) - 设置物品冷却
        runtime.registerFunction("germCooldown", listOf(2, 3)) { ctx ->
            val slot = ctx.getAsString(0) ?: return@registerFunction null
            val tick = ctx.getAsInt(1)
            val targets = ctx.getTargetsArg(2, LeastType.SENDER)

            val equipment = runCatching {
                taboolib.type.BukkitEquipment.valueOf(slot.uppercase())
            }.getOrNull() ?: return@registerFunction null

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                GermPacketAPI.setItemStackCooldown(player, equipment.getItem(player), tick)
            }
            null
        }
    }
}
