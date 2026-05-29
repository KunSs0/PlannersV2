package com.gitee.planners.core.ui

import com.gitee.planners.api.KeyBindingAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.core.player.PlayerSkill
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.ui.ClickEvent
import taboolib.platform.util.buildItem

object BackpackSkillSelectUI : SingletonChoiceUI<PlayerSkill>("backpack-skill-select.yml") {

    private val callback = mutableMapOf<Player, (PlayerSkill) -> Unit>()

    @SubscribeEvent
    fun e(e: PlayerQuitEvent) {
        callback.remove(e.player)
    }

    override fun onGenerate(player: Player, element: PlayerSkill, index: Int, slot: Int): ItemStack {
        return buildItem(KeyBindingAPI.createIconFormatter(player, element).build()) {
            val status = if (element.equipped) {
                "§a[已装备] §7${element.backpackPage}:${element.backpackSlot}"
            } else {
                "§7[未装备]"
            }
            lore += listOf("", status)
        }
    }

    override fun onClick(event: ClickEvent, element: PlayerSkill) {
        callback[event.clicker]?.invoke(element)
        event.clicker.closeInventory()
    }

    override fun getElements(player: Player): Collection<PlayerSkill> {
        return player.plannersTemplate.getRegisteredSkill().values
    }

    fun choice(player: Player, func: (PlayerSkill) -> Unit) {
        callback[player] = func
        openTo(player)
    }

    override fun onClose(player: Player) {
        callback.remove(player)
    }
}
