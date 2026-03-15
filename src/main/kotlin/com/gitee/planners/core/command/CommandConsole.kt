package com.gitee.planners.core.command

import com.gitee.planners.api.Registries
import com.gitee.planners.api.job.target.ProxyTarget
import org.bukkit.Bukkit
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.command.suggest
import taboolib.common5.cint

object CommandConsole {

    @CommandBody
    val cast = subCommand {
        dynamic("id") {
            suggest { Registries.SKILL.keys().toList() }

            execute<ProxyCommandSender> { sender, context, argument ->
                val skill = Registries.SKILL.get(argument)
                skill.execute(ProxyTarget.Console(Bukkit.getConsoleSender()), 1)
                sender.sendMessage("casted ${skill.id}")
            }

            dynamic("level", optional = true) {
                execute<ProxyCommandSender> { sender, context, argument ->
                    val skill = Registries.SKILL.get(context["id"])
                    skill.execute(ProxyTarget.Console(Bukkit.getConsoleSender()), argument.cint)
                    sender.sendMessage("casted ${skill.id}")
                }
            }

        }
    }

}
