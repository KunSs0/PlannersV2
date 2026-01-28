package com.gitee.planners.core.command.profile

import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.core.command.withValue
import taboolib.common.platform.command.CommandBody
import taboolib.platform.util.sendLang

object CommandExperience {

    @CommandBody
    val add = withValue { player, value ->
        PlayerTemplateAPI.addExperience(player, value)
        player.sendLang("command-experience-add", player.name, value)
    }

    @CommandBody
    val take = withValue { player, value ->
        PlayerTemplateAPI.takeExperience(player, value)
        player.sendLang("command-experience-take", player.name, value)
    }

    @CommandBody
    val set = withValue { player, value ->
        PlayerTemplateAPI.setExperience(player, value)
        player.sendLang("command-experience-set", player.name, value)
    }
}
