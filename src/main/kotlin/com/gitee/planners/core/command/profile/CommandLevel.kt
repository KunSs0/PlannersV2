package com.gitee.planners.core.command.profile

import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.core.command.withValue
import taboolib.common.platform.command.CommandBody
import taboolib.platform.util.sendLang

object CommandLevel {

    @CommandBody
    val add = withValue { player, value ->
        PlayerTemplateAPI.addLevel(player, value)
        player.sendLang("command-level-add", player.name, value)
    }

    @CommandBody
    val take = withValue { player, value ->
        PlayerTemplateAPI.addLevel(player, -value)
        player.sendLang("command-level-take", player.name, value)
    }

    @CommandBody
    val set = withValue { player, value ->
        PlayerTemplateAPI.setLevel(player, value)
        player.sendLang("command-level-set", player.name, value)
    }
}
