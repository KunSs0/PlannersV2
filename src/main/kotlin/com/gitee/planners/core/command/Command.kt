package com.gitee.planners.core.command

import com.gitee.planners.api.RegistryBuiltin
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.expansion.createHelper

@CommandHeader("planners", aliases = ["pl", "ps"], permission = "planners.command")
object Command {

    @CommandBody
    val main = mainCommand { createHelper() }

    @CommandBody
    val skill = CommandSkill

    @CommandBody
    val route = CommandRoute

    @CommandBody
    val reload = subCommand {
        execute<ProxyCommandSender> { sender, context, argument ->
            RegistryBuiltin.handleReload()
            sender.sendMessage("Reloaded.")
        }
    }

}
