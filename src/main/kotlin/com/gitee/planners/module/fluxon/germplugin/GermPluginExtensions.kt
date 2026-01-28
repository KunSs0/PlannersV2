package com.gitee.planners.module.fluxon.germplugin

import com.germ.germplugin.api.GermPacketAPI
import com.germ.germplugin.api.SoundType
import com.germ.germplugin.api.ViewType
import com.germ.germplugin.api.bean.AnimDataDTO
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache

import com.gitee.planners.module.fluxon.getTargetsArg
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
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

        // germEffect(name) - 播放特效
        runtime.registerFunction("germEffect", returns(Type.STRING).params(Type.STRING)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val index = UUID.randomUUID().toString()
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            playEffect(name, index, targets)
            index
        }

        // germEffect(name, index) - 播放特效，指定索引
        runtime.registerFunction("germEffect", returns(Type.STRING).params(Type.STRING, Type.STRING)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val index = ctx.getString(1) ?: UUID.randomUUID().toString()
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            playEffect(name, index, targets)
            index
        }

        // germEffect(name, index, targets) - 播放特效给目标
        runtime.registerFunction("germEffect", returns(Type.STRING).params(Type.STRING, Type.STRING, Type.OBJECT)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val index = ctx.getString(1) ?: UUID.randomUUID().toString()
            val targets = ctx.getTargetsArg(2, LeastType.SENDER)
            playEffect(name, index, targets)
            index
        }

        // germEffectRemove(index) - 移除特效
        runtime.registerFunction("germEffectRemove", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val index = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                GermPacketAPI.removeEffect(player, index)
            }
            null
        }

        // germEffectRemove(index, targets) - 移除目标特效
        runtime.registerFunction("germEffectRemove", returns(Type.VOID).params(Type.STRING, Type.OBJECT)) { ctx ->
            val index = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                GermPacketAPI.removeEffect(player, index)
            }
            null
        }

        // germEffectClear() - 清除所有特效
        runtime.registerFunction("germEffectClear", returns(Type.VOID).noParams()) { ctx ->
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                GermPacketAPI.clearEffect(player)
            }
            null
        }

        // germEffectClear(targets) - 清除目标所有特效
        runtime.registerFunction("germEffectClear", returns(Type.VOID).params(Type.OBJECT)) { ctx ->
            val targets = ctx.getTargetsArg(0, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                GermPacketAPI.clearEffect(player)
            }
            null
        }

        // germSound(name) - 播放声音
        runtime.registerFunction("germSound", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            playGermSound(name, SoundType.MASTER, 1f, 1f, targets)
            null
        }

        // germSound(name, type, volume, pitch) - 播放声音，全参数
        runtime.registerFunction("germSound", returns(Type.VOID).params(Type.STRING, Type.STRING, Type.F, Type.F)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val typeName = ctx.getString(1) ?: "MASTER"
            val volume = ctx.getFloat(2)
            val pitch = ctx.getFloat(3)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            val type = runCatching { SoundType.valueOf(typeName.uppercase()) }.getOrElse { SoundType.MASTER }
            playGermSound(name, type, volume, pitch, targets)
            null
        }

        // germSound(name, type, volume, pitch, targets) - 播放声音给目标
        runtime.registerFunction("germSound", returns(Type.VOID).params(Type.STRING, Type.STRING, Type.F, Type.F, Type.OBJECT)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val typeName = ctx.getString(1) ?: "MASTER"
            val volume = ctx.getFloat(2)
            val pitch = ctx.getFloat(3)
            val targets = ctx.getTargetsArg(4, LeastType.SENDER)
            val type = runCatching { SoundType.valueOf(typeName.uppercase()) }.getOrElse { SoundType.MASTER }
            playGermSound(name, type, volume, pitch, targets)
            null
        }

        // germAnimation(name) - 播放动画
        runtime.registerFunction("germAnimation", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            playAnimation(name, 1f, false, targets)
            null
        }

        // germAnimation(name, speed, reverse) - 播放动画，带速度和反转
        runtime.registerFunction("germAnimation", returns(Type.VOID).params(Type.STRING, Type.F, Type.BOOLEAN)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val speed = ctx.getFloat(1)
            val reverse = ctx.getBool(2)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            playAnimation(name, speed, reverse, targets)
            null
        }

        // germAnimation(name, speed, reverse, targets) - 播放目标动画
        runtime.registerFunction("germAnimation", returns(Type.VOID).params(Type.STRING, Type.F, Type.BOOLEAN, Type.OBJECT)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val speed = ctx.getFloat(1)
            val reverse = ctx.getBool(2)
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)
            playAnimation(name, speed, reverse, targets)
            null
        }

        // germAnimationStop(name) - 停止动画
        runtime.registerFunction("germAnimationStop", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            stopAnimation(name, targets)
            null
        }

        // germAnimationStop(name, targets) - 停止目标动画
        runtime.registerFunction("germAnimationStop", returns(Type.VOID).params(Type.STRING, Type.OBJECT)) { ctx ->
            val name = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            stopAnimation(name, targets)
            null
        }

        // germViewLock() - 锁定视角
        runtime.registerFunction("germViewLock", returns(Type.VOID).noParams()) { ctx ->
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            lockView(-1L, ViewType.FIRST_PERSON, targets)
            null
        }

        // germViewLock(duration, type) - 锁定视角，带时长和类型
        runtime.registerFunction("germViewLock", returns(Type.VOID).params(Type.J, Type.STRING)) { ctx ->
            val duration = ctx.getAsLong(0)
            val typeName = ctx.getString(1) ?: "FIRST_PERSON"
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            val type = runCatching { ViewType.valueOf(typeName.uppercase()) }.getOrElse { ViewType.FIRST_PERSON }
            lockView(duration, type, targets)
            null
        }

        // germViewLock(duration, type, targets) - 锁定目标视角
        runtime.registerFunction("germViewLock", returns(Type.VOID).params(Type.J, Type.STRING, Type.OBJECT)) { ctx ->
            val duration = ctx.getAsLong(0)
            val typeName = ctx.getString(1) ?: "FIRST_PERSON"
            val targets = ctx.getTargetsArg(2, LeastType.SENDER)
            val type = runCatching { ViewType.valueOf(typeName.uppercase()) }.getOrElse { ViewType.FIRST_PERSON }
            lockView(duration, type, targets)
            null
        }

        // germViewUnlock() - 解锁视角
        runtime.registerFunction("germViewUnlock", returns(Type.VOID).noParams()) { ctx ->
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                GermPacketAPI.sendUnlockPlayerCameraView(player)
            }
            null
        }

        // germViewUnlock(targets) - 解锁目标视角
        runtime.registerFunction("germViewUnlock", returns(Type.VOID).params(Type.OBJECT)) { ctx ->
            val targets = ctx.getTargetsArg(0, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                GermPacketAPI.sendUnlockPlayerCameraView(player)
            }
            null
        }

        // germLookLock() - 锁定视线
        runtime.registerFunction("germLookLock", returns(Type.VOID).noParams()) { ctx ->
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            lockLook(-1L, targets)
            null
        }

        // germLookLock(duration) - 锁定视线，带时长
        runtime.registerFunction("germLookLock", returns(Type.VOID).params(Type.J)) { ctx ->
            val duration = ctx.getAsLong(0)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            lockLook(duration, targets)
            null
        }

        // germLookLock(duration, targets) - 锁定目标视线
        runtime.registerFunction("germLookLock", returns(Type.VOID).params(Type.J, Type.OBJECT)) { ctx ->
            val duration = ctx.getAsLong(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            lockLook(duration, targets)
            null
        }

        // germLookUnlock() - 解锁视线
        runtime.registerFunction("germLookUnlock", returns(Type.VOID).noParams()) { ctx ->
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                GermPacketAPI.sendUnlockPlayerCameraRotate(player)
            }
            null
        }

        // germLookUnlock(targets) - 解锁目标视线
        runtime.registerFunction("germLookUnlock", returns(Type.VOID).params(Type.OBJECT)) { ctx ->
            val targets = ctx.getTargetsArg(0, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                GermPacketAPI.sendUnlockPlayerCameraRotate(player)
            }
            null
        }

        // germMoveLock() - 锁定移动
        runtime.registerFunction("germMoveLock", returns(Type.VOID).noParams()) { ctx ->
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            lockMove(-1L, targets)
            null
        }

        // germMoveLock(duration) - 锁定移动，带时长
        runtime.registerFunction("germMoveLock", returns(Type.VOID).params(Type.J)) { ctx ->
            val duration = ctx.getAsLong(0)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            lockMove(duration, targets)
            null
        }

        // germMoveLock(duration, targets) - 锁定目标移动
        runtime.registerFunction("germMoveLock", returns(Type.VOID).params(Type.J, Type.OBJECT)) { ctx ->
            val duration = ctx.getAsLong(0)
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            lockMove(duration, targets)
            null
        }

        // germMoveUnlock() - 解锁移动
        runtime.registerFunction("germMoveUnlock", returns(Type.VOID).noParams()) { ctx ->
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                GermPacketAPI.sendUnlockPlayerMove(player)
            }
            null
        }

        // germMoveUnlock(targets) - 解锁目标移动
        runtime.registerFunction("germMoveUnlock", returns(Type.VOID).params(Type.OBJECT)) { ctx ->
            val targets = ctx.getTargetsArg(0, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                GermPacketAPI.sendUnlockPlayerMove(player)
            }
            null
        }

        // germCooldown(slot, tick) - 设置物品冷却
        runtime.registerFunction("germCooldown", returns(Type.VOID).params(Type.STRING, Type.I)) { ctx ->
            val slot = ctx.getString(0) ?: return@registerFunction
            val tick = ctx.getAsInt(1)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            setCooldown(slot, tick, targets)
            null
        }

        // germCooldown(slot, tick, targets) - 设置目标物品冷却
        runtime.registerFunction("germCooldown", returns(Type.VOID).params(Type.STRING, Type.I, Type.OBJECT)) { ctx ->
            val slot = ctx.getString(0) ?: return@registerFunction
            val tick = ctx.getAsInt(1)
            val targets = ctx.getTargetsArg(2, LeastType.SENDER)
            setCooldown(slot, tick, targets)
            null
        }
    }

    /**
     * 播放 GermPlugin 特效
     * @param name 特效名称
     * @param index 特效索引 ID
     * @param targets 目标容器（支持实体和位置）
     */
    private fun playEffect(name: String, index: String, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
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
    }

    /**
     * 播放 GermPlugin 声音
     * @param name 声音名称
     * @param type 声音类型
     * @param volume 音量
     * @param pitch 音调
     * @param targets 目标容器（支持实体和位置）
     */
    private fun playGermSound(name: String, type: SoundType, volume: Float, pitch: Float, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
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
    }

    /**
     * 播放 GermPlugin 动画
     * @param name 动画名称
     * @param speed 播放速度
     * @param reverse 是否反向播放
     * @param targets 目标实体容器
     */
    private fun playAnimation(name: String, speed: Float, reverse: Boolean, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
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
    }

    /**
     * 停止 GermPlugin 动画
     * @param name 动画名称
     * @param targets 目标实体容器
     */
    private fun stopAnimation(name: String, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
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
    }

    /**
     * 锁定玩家视角
     * @param duration 锁定时长（tick，-1 为永久）
     * @param type 视角类型
     * @param targets 目标玩家容器
     */
    private fun lockView(duration: Long, type: ViewType, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val player = target.instance as? Player ?: return@forEach
            GermPacketAPI.sendLockPlayerCameraView(player, type, duration)
        }
    }

    /**
     * 锁定玩家视线旋转
     * @param duration 锁定时长（tick，-1 为永久）
     * @param targets 目标玩家容器
     */
    private fun lockLook(duration: Long, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val player = target.instance as? Player ?: return@forEach
            GermPacketAPI.sendLockPlayerCameraRotate(player, duration)
        }
    }

    /**
     * 锁定玩家移动
     * @param duration 锁定时长（tick，-1 为永久）
     * @param targets 目标玩家容器
     */
    private fun lockMove(duration: Long, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val player = target.instance as? Player ?: return@forEach
            GermPacketAPI.sendLockPlayerMove(player, duration)
        }
    }

    /**
     * 设置玩家物品槽位冷却
     * @param slot 物品槽位名称（如 HAND, OFF_HAND, HELMET 等）
     * @param tick 冷却时长（tick）
     * @param targets 目标玩家容器
     */
    private fun setCooldown(slot: String, tick: Int, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        val equipment = runCatching {
            taboolib.type.BukkitEquipment.valueOf(slot.uppercase())
        }.getOrNull() ?: return

        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            val player = target.instance as? Player ?: return@forEach
            GermPacketAPI.setItemStackCooldown(player, equipment.getItem(player), tick)
        }
    }
}
