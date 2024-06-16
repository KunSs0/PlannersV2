package com.gitee.planners.core.command

import com.gitee.planners.api.PlannersAPI
import com.gitee.planners.api.Registries
import com.gitee.planners.core.ui.PlayerSkillOperatorUI
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.*
import taboolib.common5.cint

object CommandSkill {

    @CommandBody
    val open = with { player ->
        PlayerSkillOperatorUI.openTo(player)
    }

    @CommandBody
    val cast = Command.withPlayerSkill { player, skill ->
        PlannersAPI.cast(player,skill)
    }

    @CommandBody
    val run = subCommand {
        dynamic("player") {
            suggestPlayers()

            dynamic("skill") {
                suggest { Registries.SKILL.keys().toList() }

                execute<ProxyCommandSender> { sender, context, argument ->
                    PlannersAPI.cast(context.getBukkitPlayer()!!, Registries.SKILL.get(argument), 1)
                }

                dynamic("level", optional = true) {

                    execute<ProxyCommandSender> { sender, context, argument ->
                        val player = context.getBukkitPlayer()!!
                        val skill = Registries.SKILL.get(context["skill"])
                        val level = argument.cint
                        PlannersAPI.cast(player, skill, level)
                    }

                }
            }

        }
    }

}
