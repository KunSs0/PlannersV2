package com.gitee.planners.core.command

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.core.ui.RouteTransferUI
import com.gitee.planners.core.ui.RouterSelectUI
import taboolib.common.platform.command.CommandBody
import taboolib.platform.util.sendLang

object CommandRoute {

    @CommandBody
    val open = with { player ->
        RouterSelectUI.openTo(player)
    }

    @CommandBody
    val transfer = with { player ->
        val plannersTemplate = player.plannersTemplate
        if (plannersTemplate.route == null) {
            player.sendLang("player-route-invalid")
            return@with
        }
        RouteTransferUI.openTo(player)
    }

    @CommandBody
    val clear = with { player ->
        player.plannersTemplate.route = null
        player.sendLang("player-route-clear")
    }


}
