package com.gitee.planners.module.compat.protect

import com.gitee.planners.api.event.player.TargetCapturedEvent
import com.gitee.planners.util.checkPlugin
import org.bukkit.entity.Player
import org.serverct.ersha.dungeon.DungeonPlus
import org.serverct.ersha.dungeon.common.team.Team
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.util.unsafeLazy
import taboolib.common5.Demand

/**
 * 玩家保护攻击在地牢场景处理器
 */
object PlayerCapturedOnDungeonPlusSceneHandler {

    private val isPluginLoaded by unsafeLazy {
        checkPlugin("DungeonPlus")
    }

    const val SCENE_DUNGEON_NAME = "dungeonplus"

    const val SCENE_TEAM_NAME = "team"

    @SubscribeEvent
    fun e1(e: TargetCapturedEvent) {
        // 只有在插件加载成功且玩家是被攻击时才会执行
        if (!isPluginLoaded || e.cause != TargetCapturedEvent.DAMAGED || e.container.none { it.instance is Player }) {
            return
        }
        val scenes = PlayerProtectAttackHandler.getScenes(SCENE_DUNGEON_NAME)
        // 如果没有地牢场景，则取消本次检索
        if (scenes.isEmpty()) {
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


    @SubscribeEvent
    fun e2(e: TargetCapturedEvent) {
        // 只有在插件加载成功且玩家是被攻击时才会执行
        if (!isPluginLoaded || e.cause != TargetCapturedEvent.DAMAGED || e.container.none { it.instance is Player }) {
            return
        }
        val sender = e.sender.instance as? Player
        if (sender == null) {
            return
        }
        // 必须是同队玩家
        val team = DungeonPlus.teamManager.getTeam(sender)
        if (team == null) {
            return
        }
        val scenes = PlayerProtectAttackHandler.getScenes(SCENE_TEAM_NAME)
        // 如果没有地牢场景，则取消本次检索
        if (scenes.isEmpty()) {
            return
        }
        e.container.removeIf {
            if (it.instance !is Player) {
                return@removeIf false
            }
            val player = it.instance as Player

            // 检索队伍场景
            return@removeIf checkTeam(player, team, scenes)
        }
    }


    fun checkWorld(player: Player, scenes: List<Demand>): Boolean {
        val dungeon = DungeonPlus.dungeonManager.getDungeon(player)
        if (dungeon == null) {
            return false
        }


        return scenes.any { scene -> scene.args.contains("*") || scene.args.contains(dungeon.dungeonName) }
    }

    fun checkTeam(player: Player, source: Team, scenes: List<Demand>): Boolean {
        val team = DungeonPlus.teamManager.getTeam(player)

        return team != null && scenes.isNotEmpty() && team == source
    }

}