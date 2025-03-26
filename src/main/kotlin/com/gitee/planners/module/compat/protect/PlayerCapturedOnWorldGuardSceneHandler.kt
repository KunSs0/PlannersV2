package com.gitee.planners.module.compat.protect

import com.gitee.planners.api.event.player.TargetCapturedEvent
import com.gitee.planners.module.compat.worldguard.WorldGuardAccess
import com.gitee.planners.util.checkPlugin
import org.bukkit.entity.Player
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.util.unsafeLazy
import taboolib.common5.Demand

object PlayerCapturedOnWorldGuardSceneHandler {

    private val isPluginLoaded by unsafeLazy {
        checkPlugin("WorldGuard")
    }

    const val SCENE_WORLDGUARD_NAME = "worldguard"

    @SubscribeEvent
    fun e(e: TargetCapturedEvent) {
        // 只有在插件加载成功且玩家是被攻击时才会执行
        if (!isPluginLoaded || e.cause != TargetCapturedEvent.DAMAGED || e.container.none { it.instance is Player }) {
            return
        }
        val scenes = PlayerProtectAttackHandler.getScenes(SCENE_WORLDGUARD_NAME)
        // 如果没有地牢场景，则取消本次检索
        if (scenes.isEmpty()) {
            return
        }
        val sender = e.sender.instance as? Player
        // 如果释放者在区域内，则直接删除容器内所有玩家目标
        if (sender != null && checkWorld(sender, scenes)) {
            e.container.removeIf { it.instance is Player }
            return
        }
        e.container.removeIf {
            if (it.instance !is Player) {
                return@removeIf false
            }
            val player = it.instance as Player


            // 检索世界场景
            return@removeIf checkWorld(player, scenes)
        }
    }

    private fun checkWorld(player: Player, scenes: List<Demand>): Boolean {
        val regions = WorldGuardAccess.INSTANCE!!.getRegions(player)
        if (regions.isEmpty()) {
            return false
        }

        // 如果存在保护方案，且在保护方案内，则移除
        return regions.any { region ->
            scenes.any { it.args.contains("*") || it.args.contains(region) }
        }
    }

}