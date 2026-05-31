package com.gitee.planners.core.skill.binding

import com.gitee.planners.api.BackpackAPI
import com.gitee.planners.api.KeyBindingAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.Registries
import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.api.event.player.PlayerProfileLoadedEvent
import com.gitee.planners.api.event.player.PlayerSkillEvent
import com.gitee.planners.api.job.KeyBinding
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.util.configNodeTo
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.event.SubscribeEvent
import taboolib.library.xseries.XItemStack
import taboolib.module.configuration.ConfigNode
import taboolib.platform.util.onlinePlayers

object MinecraftInteraction {

    @ConfigNode("settings.minecraft.interaction-action.enable")
    val isEnable = false

    @ConfigNode("settings.minecraft.interaction-action.empty-skill")
    val emptySkill = configNodeTo {
        XItemStack.deserialize(this)
    }

    fun clearInventory(player: Player) {
        Registries.KEYBINDING.values().forEachIndexed { index, _ ->
            player.inventory.setItem(index, null)
        }
    }

    fun updateInventory(player: Player) = updateInventory(player.plannersTemplate)

    fun updateInventory(template: PlayerTemplate) {
        val player = template.onlinePlayer
        val currentPage = BackpackAPI.getCurrentPage(template)
        val pageConfig = Registries.BACKPACK.getPage(currentPage) ?: return

        // 清空所有 keybinding 对应的 hotbar 槽位
        clearInventory(player)

        // 按当前页的槽位填充
        pageConfig.slots.forEach { (slotId, slotConfig) ->
            val keybinding = Registries.KEYBINDING.getOrNull(slotConfig.key) ?: return@forEach
            val skill = template.getEquippedSkillByBackpackSlot(currentPage, slotId)
            if (skill != null) {
                val formatter = KeyBindingAPI.createIconFormatter(player, skill)
                val index = Registries.KEYBINDING.values().indexOf(keybinding)
                player.inventory.setItem(index, formatter.build())
            }
        }
    }

    fun updateInventory(template: PlayerTemplate, skill: PlayerSkill) {
        if (!skill.equipped || skill.backpackPage == null || skill.backpackSlot == null) return
        val pageConfig = Registries.BACKPACK.getPage(skill.backpackPage!!) ?: return
        val slotConfig = pageConfig.slots[skill.backpackSlot!!] ?: return
        val keybinding = Registries.KEYBINDING.getOrNull(slotConfig.key) ?: return
        val player = template.onlinePlayer
        val formatter = KeyBindingAPI.createIconFormatter(player, skill)
        val index = Registries.KEYBINDING.values().indexOf(keybinding)
        player.inventory.setItem(index, formatter.build())
    }

    fun execute(func: () -> Unit) {
        if (isEnable) func()
    }

    @SubscribeEvent
    fun e(e: PlayerProfileLoadedEvent) {
        execute {
            updateInventory(e.template)
        }
    }

    @SubscribeEvent
    fun e(e: PlayerSkillEvent.LevelChange) {
        execute {
            updateInventory(e.template, e.skill)
        }
    }

    @SubscribeEvent
    fun e(e: PluginReloadEvents.Pre) {
        execute {
            onlinePlayers.forEach { clearInventory(it) }
        }
    }

    @SubscribeEvent
    fun e(e: PluginReloadEvents.Post) {
        execute {
            onlinePlayers.forEach { updateInventory(it) }
        }
    }

}
