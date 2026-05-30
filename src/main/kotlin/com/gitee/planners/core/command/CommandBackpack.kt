package com.gitee.planners.core.command

import com.gitee.planners.api.BackpackAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.Registries
import com.gitee.planners.core.skill.binding.MinecraftInteraction
import com.gitee.planners.core.ui.BackpackUI
import org.bukkit.entity.Player
import taboolib.common.platform.command.*
import taboolib.platform.util.sendLang

object CommandBackpack {

    @CommandBody
    val open = with { player ->
        BackpackUI.openTo(player)
    }

    @CommandBody
    val page = subCommand {
        dynamic("page") {
            suggest { Registries.BACKPACK.pages.keys.toList() }
            execute<Player> { player, context, _ ->
                val pageId = context["page"]
                val template = player.plannersTemplate
                BackpackAPI.setCurrentPage(template, pageId)
                MinecraftInteraction.updateInventory(template)
                player.sendLang("command-backpack-switched", Registries.BACKPACK.getPage(pageId)?.name ?: pageId)
            }
        }
    }
}
