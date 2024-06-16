package com.gitee.planners.core.ui

import com.gitee.planners.api.ProfileAPI
import com.gitee.planners.api.ProfileAPI.plannersProfile
import com.gitee.planners.api.Registries
import com.gitee.planners.core.config.ImmutableRouter
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.module.ui.ClickEvent
import taboolib.platform.util.buildItem
import taboolib.platform.util.sendLang

object RouterSelectUI : SingletonChoiceUI<ImmutableRouter>("router-select.yml") {

    override fun getElements(player: Player): Collection<ImmutableRouter> {
        return Registries.ROUTER.values()
    }

    override fun onGenerate(player: Player, element: ImmutableRouter, index: Int, slot: Int): ItemStack {
        return element.icon ?: buildItem(Material.STONE) { name = element.name }
    }

    override fun onClick(event: ClickEvent, element: ImmutableRouter) {
        val player = event.clicker
        val profile = player.plannersProfile
        if (profile.route != null) {
            player.sendLang("player-route-exists", profile.route!!.name)
            return
        }
        event.clicker.closeInventory()
        val route = element.originate ?: error("Route originate does not exist")
        // 选择职业

        ProfileAPI.OPERATOR.createPlayerRoute(profile, route).thenAccept {
            profile.route = it
            player.sendLang("player-route-selected", it.name)
        }
    }
}
