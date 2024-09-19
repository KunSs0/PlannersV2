package com.gitee.planners.core.ui

import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.Registries
import com.gitee.planners.core.config.ImmutableRouter
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.module.ui.ClickEvent
import taboolib.platform.util.buildItem
import taboolib.platform.util.sendLang

object PlayerRouterSelectUI : SingletonChoiceUI<ImmutableRouter>("router-select.yml") {

    override fun getElements(player: Player): Collection<ImmutableRouter> {
        return Registries.ROUTER.values()
    }

    override fun onGenerate(player: Player, element: ImmutableRouter, index: Int, slot: Int): ItemStack {
        return element.icon ?: buildItem(Material.STONE) { name = element.name }
    }

    override fun onClick(event: ClickEvent, element: ImmutableRouter) {
        val player = event.clicker
        val template = player.plannersTemplate
        if (template.route != null) {
            player.sendLang("player-route-exists", template.route!!.name)
            return
        }
        event.clicker.closeInventory()
        val route = element.originate ?: error("Route originate does not exist")
        // 选择职业
        PlayerTemplateAPI.setPlayerRoute(player, route).thenAccept {
            if (it != null) {
                player.sendLang("player-route-selected", it.name)
            }
        }
    }
}
