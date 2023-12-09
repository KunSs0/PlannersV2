package com.gitee.planners.core.ui

import com.gitee.planners.api.ProfileAPI
import com.gitee.planners.api.RegistryBuiltin
import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.config.ImmutableRouter
import com.gitee.planners.util.configNodeToList
import org.bukkit.Material
import org.bukkit.entity.Player
import taboolib.common5.cint
import taboolib.platform.util.buildItem
import taboolib.platform.util.sendLang

object RouterSelectUI : AutomationBaseUI("router-select.yml") {

    @BaseUI.Option("__option__.rows")
    val rows = 6

    @BaseUI.Option("__option__.title")
    val title = "Chest"

    @BaseUI.Option("__option__.slots")
    val slots = simpleConfigNodeTo<List<Any>, List<Int>> {
        map { it.cint }
    }

    @BaseUI.Option("*")
    val decorateIcon = decorateIcon()

    override fun display(player: Player): BaseUI.Display {

        return BaseUI.linked<ImmutableRouter>(title) {
            rows(this@RouterSelectUI.rows)
            slots(this@RouterSelectUI.slots.get())
            elements { RegistryBuiltin.ROUTER.getValues() }
            // 注入装饰品
            injectDecorateIcon(decorateIcon.get())

            onGenerate { player, element, _, _ ->
                element.icon ?: buildItem(Material.STONE) { name = element.name }
            }

            onClick { event, element ->
                ProfileAPI.modified(player) {
                    if (this.getRoute() != null) {
                        player.sendLang("player-route-exists", this.getRoute()!!.name)
                        return@modified
                    }
                    val route = element.originate ?: error("Route originate does not exist")
                    // 选择职业
                    this.setRoute(route).thenAccept {
                        player.sendLang("player-route-selected", getRoute()!!.name)
                    }
                }
            }

        }
    }

}
