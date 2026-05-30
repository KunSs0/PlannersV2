package com.gitee.planners.core.ui

import com.gitee.planners.api.KeyBindingAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.core.player.PlayerSkill
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.util.asList
import taboolib.common.util.replaceWithOrder
import taboolib.module.ui.ClickEvent
import taboolib.platform.util.asLangText
import taboolib.platform.util.buildItem

object PlayerSkillOperatorUI : SingletonChoiceUI<PlayerSkill>("skill-operator.yml") {

    @Option("__option__.append-text")
    val appendString = simpleConfigNodeTo<List<*>, List<String>> {
        this.asList()
    }

    override fun onGenerate(player: Player, element: PlayerSkill, index: Int, slot: Int): ItemStack {
        return buildItem(KeyBindingAPI.createIconFormatter(player, element).build()) {
            val slotInfo = if (element.equipped) {
                player.asLangText("skill-operator-equipped", element.backpackPage ?: "?", element.backpackSlot ?: "?")
            } else {
                player.asLangText("skill-operator-unequipped")
            }
            lore += appendString.get().map { it.replaceWithOrder(slotInfo) }
        }
    }

    override fun onClick(event: ClickEvent, element: PlayerSkill) {
        // 打开技能背包
        BackpackUI.openTo(event.clicker)
    }

    override fun getElements(player: Player): Collection<PlayerSkill> {
        return player.plannersTemplate.getRegisteredSkill().values
    }

}
