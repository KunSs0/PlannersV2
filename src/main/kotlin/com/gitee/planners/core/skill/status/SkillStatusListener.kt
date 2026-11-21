package com.gitee.planners.core.skill.status

import com.gitee.planners.api.event.player.BackpackEquipEvent
import com.gitee.planners.api.event.player.PlayerRouteClearedEvent
import com.gitee.planners.api.event.player.PlayerSetRouteEvent
import com.gitee.planners.api.event.player.PlayerSkillCastEvent
import com.gitee.planners.api.event.player.PlayerSkillCooldownEvent
import com.gitee.planners.api.event.player.PlayerSkillTreeNodeEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

/**
 * 技能状态失效监听。
 *
 * 只在"玩家状态可能变化"时打脏标记，具体计算统一由 [PlayerSkillStatus] 合并后执行：
 * 释放成功（资源已扣）、冷却写入、装备/卸下、职业与技能树变化、退出清理。
 */
object SkillStatusListener : Listener {

    private var registered = false

    /** 注册到 Bukkit 事件总线，重复调用幂等。 */
    fun register() {
        if (registered) {
            return
        }
        val plugin = Bukkit.getPluginManager().getPlugin("Planners")
        if (plugin == null) {
            return
        }
        Bukkit.getPluginManager().registerEvents(this, plugin)
        registered = true
    }

    /**
     * 释放成功后资源已扣除。
     *
     * @param event 释放事件。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onCast(event: PlayerSkillCastEvent.Post) {
        PlayerSkillStatus.invalidate(event.player, "cast")
    }

    /**
     * 冷却被写入，可能是脚本或外部入口设置的。
     *
     * @param event 冷却事件。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onCooldown(event: PlayerSkillCooldownEvent.Set) {
        PlayerSkillStatus.invalidate(event.player, "cooldown")
    }

    /**
     * 装备技能。
     *
     * @param event 装备事件。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onEquip(event: BackpackEquipEvent.Post) {
        PlayerSkillStatus.invalidate(event.player, "equip")
    }

    /**
     * 卸下技能。
     *
     * @param event 卸下事件。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onUnequip(event: BackpackEquipEvent.Unequip) {
        PlayerSkillStatus.invalidate(event.player, "equip")
    }

    /**
     * 技能树等级变化。
     *
     * @param event 节点事件。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onSkillTreeNode(event: PlayerSkillTreeNodeEvent) {
        PlayerSkillStatus.invalidate(event.player, "skill-tree")
    }

    /**
     * 切换职业。
     *
     * @param event 切换事件。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onSetRoute(event: PlayerSetRouteEvent.Post) {
        PlayerSkillStatus.invalidate(event.player, "route")
    }

    /**
     * 清空职业：缓存整体作废。
     *
     * @param event 清空事件。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onRouteCleared(event: PlayerRouteClearedEvent) {
        PlayerSkillStatus.clear(event.player)
    }

    /**
     * 退出清理。
     *
     * @param event 退出事件。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        PlayerSkillStatus.clear(event.player)
    }
}
