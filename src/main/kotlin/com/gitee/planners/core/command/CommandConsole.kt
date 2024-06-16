package com.gitee.planners.core.command

import com.gitee.planners.api.Registries
import com.gitee.planners.module.kether.context.ImmutableSkillContext
import com.gitee.planners.api.job.target.adaptTarget
import org.bukkit.command.ConsoleCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.command.suggest
import taboolib.common5.cint

object CommandConsole {

    @CommandBody
    val cast = subCommand {
        dynamic("id") {
            suggest { Registries.SKILL.keys().toList() }

            execute<ConsoleCommandSender> { sender, context, argument ->
                val skill = Registries.SKILL.get(argument)
                ImmutableSkillContext(sender.adaptTarget(), skill, 1).call()
                sender.sendMessage("casted ${skill.id}")
            }

            dynamic("level", optional = true) {
                execute<ConsoleCommandSender> { sender, context, argument ->
                    val skill = Registries.SKILL.get(context["id"])
                    ImmutableSkillContext(sender.adaptTarget(), skill, argument.cint).call()
                    sender.sendMessage("casted ${skill.id}")
                }
            }

        }
    }

}
