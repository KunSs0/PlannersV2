package com.gitee.planners.core.ui

import com.gitee.planners.api.KeyBindingAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.core.config.SkillCategories
import com.gitee.planners.core.player.PlayerSkill
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.ui.ClickEvent
import taboolib.platform.util.asLangText
import taboolib.platform.util.buildItem

object BackpackSkillSelectUI : SingletonChoiceUI<PlayerSkill>("backpack-skill-select.yml") {

    private val callback = mutableMapOf<Player, (PlayerSkill) -> Unit>()
    private val categoryFilters = mutableMapOf<Player, Set<String>>()

    @SubscribeEvent
    fun e(e: PlayerQuitEvent) {
        callback.remove(e.player)
        categoryFilters.remove(e.player)
    }

    override fun onGenerate(player: Player, element: PlayerSkill, index: Int, slot: Int): ItemStack {
        return buildItem(KeyBindingAPI.createIconFormatter(player, element).build()) {
            val status = if (element.equipped) {
                player.asLangText("backpack-skill-equipped", element.backpackPage ?: "?", element.backpackSlot ?: "?")
            } else {
                player.asLangText("backpack-skill-unequipped")
            }
            lore += listOf("", status)
        }
    }

    override fun onClick(event: ClickEvent, element: PlayerSkill) {
        callback[event.clicker]?.invoke(element)
        event.clicker.closeInventory()
    }

    override fun getElements(player: Player): Collection<PlayerSkill> {
        val categories = categoryFilters[player]
        if (categories == null) {
            throw IllegalStateException("技能选择界面缺少槽位分类上下文")
        }

        val result = mutableListOf<PlayerSkill>()
        for (skill in player.plannersTemplate.getRegisteredSkill().values) {
            if (SkillCategories.matches(skill.immutable.categories, categories)) {
                result.add(skill)
            }
        }
        return result
    }

    fun choice(player: Player, categories: Set<String>, func: (PlayerSkill) -> Unit) {
        callback[player] = func
        categoryFilters[player] = categories
        openTo(player)
    }

    override fun onClose(player: Player) {
        callback.remove(player)
        categoryFilters.remove(player)
    }
}
