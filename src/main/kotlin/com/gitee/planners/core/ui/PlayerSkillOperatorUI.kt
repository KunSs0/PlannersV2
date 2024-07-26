package com.gitee.planners.core.ui

import com.gitee.planners.api.KeyBindingAPI
import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.core.player.PlayerSkill
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.util.asList
import taboolib.common.util.replaceWithOrder
import taboolib.module.ui.ClickEvent
import taboolib.platform.compat.VaultService
import taboolib.platform.util.buildItem

object PlayerSkillOperatorUI : SingletonChoiceUI<PlayerSkill>("skill-operator.yml") {

    @Option("__option__.append-text")
    val appendString = simpleConfigNodeTo<List<*>,List<String>> {
        this.asList()
    }

    override fun onGenerate(player: Player, element: PlayerSkill, index: Int, slot: Int): ItemStack {
        return buildItem(KeyBindingAPI.createIconFormatter(player,element).build()) {
            lore += appendString.get().map { it.replaceWithOrder(element.binding?.name ?: "-") }
        }
    }

    override fun onClick(event: ClickEvent, element: PlayerSkill) {
        val player = event.clicker
        VaultService
        // 打开快捷键修改
        if (event.clickEvent().isRightClick) {
            KeyBindingsEditorUI.choice(player) {
                // 打开ui
                this.openTo(player)
                // 解绑快捷键
                if (element.binding != null && element.binding == it) {
                    PlayerTemplateAPI.setSkillBinding(player.plannersTemplate,element,null)
                }
                // 更新快捷键
                else {
                    PlayerTemplateAPI.setSkillBinding(player.plannersTemplate,element,it)
                }
            }
        }
    }

    override fun getElements(player: Player): Collection<PlayerSkill> {
        return player.plannersTemplate.getRegisteredSkill().values
    }


}
