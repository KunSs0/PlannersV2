package com.gitee.planners.core.command.profile

import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.core.command.with
import com.gitee.planners.core.command.withValue
import taboolib.common.platform.command.CommandBody
import taboolib.platform.util.sendLang

object CommandMagicPoint {

    @CommandBody
    val add = withValue { player, value ->
        PlayerTemplateAPI.addMagicPoint(player, value)
        player.sendLang("command-magicpoint-add", player.name, value)
    }

    @CommandBody
    val take = withValue { player, value ->
        PlayerTemplateAPI.takeMagicPoint(player, value)
        player.sendLang("command-magicpoint-take", player.name, value)
    }

    @CommandBody
    val set = withValue { player, value ->
        PlayerTemplateAPI.setMagicPoint(player, value)
        player.sendLang("command-magicpoint-set", player.name, value)
    }

    @CommandBody
    val reset = with { player ->
        PlayerTemplateAPI.resetMagicPoint(player)
        player.sendLang("command-magicpoint-reset", player.name)
    }
}
