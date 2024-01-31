package com.gitee.planners.core.command

import com.gitee.planners.api.RegistryBuiltin
import com.gitee.planners.module.kether.context.ImmutableSkillContext
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.ui.PlayerSkillOperatorUI
import org.bukkit.entity.Player
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
        val context = ImmutableSkillContext(player.adaptTarget(), skill.immutable, 1)
        context.run()
        player.sendMessage("casted ${skill.id}")
    }

    @CommandBody
    val run = subCommand {
        dynamic("player") {
            suggestPlayers()

            dynamic("skill") {
                suggest { RegistryBuiltin.SKILL.getKeys().toList() }

                execute<ProxyCommandSender> { sender, context, argument ->
                    commandSkillRun(context.getBukkitPlayer()!!,RegistryBuiltin.SKILL.get(argument),1)
                }

                dynamic("level",optional = true) {

                    execute<ProxyCommandSender>{sender, context, argument ->
                        val player = context.getBukkitPlayer()!!
                        val skill = RegistryBuiltin.SKILL.get(context["skill"])
                        val level = argument.cint
                        commandSkillRun(player, skill, level)
                    }

                }
            }

        }
    }

    private fun commandSkillRun(player: Player,skill: ImmutableSkill,level: Int) {
        ImmutableSkillContext(player.adaptTarget(), skill, level).run()
    }


}
