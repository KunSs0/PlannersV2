package com.gitee.planners.core.command

import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.core.ui.PlayerRouteTransferUI
import com.gitee.planners.core.ui.PlayerRouterSelectUI
import taboolib.common.platform.command.CommandBody
import taboolib.platform.util.sendLang

object CommandRoute {

    @CommandBody
    val open = with { player ->
        PlayerRouterSelectUI.openTo(player)
    }

    @CommandBody
    val select = withInImmutableRouter { player, element ->
        val route = element.originate ?: error("Route originate does not exist")
        // 选择职业


        PlayerTemplateAPI.setPlayerRoute(player, route).thenAccept {
            if (it != null) {
                player.sendLang("player-route-selected", it.name)
            }
        }
    }

    @CommandBody
    val transfer = with { player ->
        val plannersTemplate = player.plannersTemplate
        if (plannersTemplate.route == null) {
            player.sendLang("player-route-invalid")
            return@with
        }
        PlayerRouteTransferUI.openTo(player)
    }

    @CommandBody
    val clear = with { player ->
        player.plannersTemplate.route = null
        player.sendLang("player-route-clear")
    }


}
