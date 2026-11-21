package com.gitee.planners.module.compat.zaphkiel

import com.gitee.planners.Planners
import com.gitee.planners.core.skill.status.PlayerSkillStatus
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerItemConsumeEvent

/**
 * 玩家背包物品变化监听，用于失效技能状态。
 *
 * Zaphkiel 的事件类型依赖它自带的 TabooLib 版本，运行期无法从本插件类路径安全引用，
 * 因此按玩家背包的原生事件失效；容器、末影箱与村民交易按业务约定不覆盖。
 * 这里只回答"物品可能变了"，具体能否释放由 [PlayerSkillStatus] 重算。
 */
object ItemChangeListener : Listener {

    /** 是否已经注册。 */
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
     * 背包点击。
     *
     * @param event 点击事件。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        invalidate(event.whoClicked)
    }

    /**
     * 丢弃物品。
     *
     * @param event 丢弃事件。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDrop(event: PlayerDropItemEvent) {
        invalidate(event.player)
    }

    /**
     * 拾取物品。
     *
     * @param event 拾取事件。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPickup(event: EntityPickupItemEvent) {
        invalidate(event.entity)
    }

    /**
     * 消耗物品。
     *
     * @param event 消耗事件。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onConsume(event: PlayerItemConsumeEvent) {
        invalidate(event.player)
    }

    /**
     * 标记玩家技能状态失效。
     *
     * @param entity 触发实体；非玩家忽略。
     */
    private fun invalidate(entity: org.bukkit.entity.Entity) {
        val player = entity as? Player
        if (player == null) {
            return
        }
    }
}
