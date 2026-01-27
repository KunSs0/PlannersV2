package com.gitee.planners.core.command

import com.gitee.planners.api.job.target.asTarget
import com.gitee.planners.core.skill.entity.state.States
import org.bukkit.entity.Player
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.player
import taboolib.common.platform.command.subCommand

object CommandState {

    // pl state trigger <sender> <name>
    @CommandBody
    val trigger = subCommand {
        dynamic("player") {

            dynamic("name") {
                execute<ProxyCommandSender> { sender, context, argument ->
                    val player = context.player("player").castSafely<Player>()!!

                    States.trigger(player.asTarget(), argument)
                }
            }
        }
    }

}