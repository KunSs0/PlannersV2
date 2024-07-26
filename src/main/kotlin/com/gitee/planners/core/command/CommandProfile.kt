package com.gitee.planners.core.command

import com.gitee.planners.api.PlayerTemplateAPI
import org.bukkit.entity.Player
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.*
import taboolib.common5.cint
import taboolib.platform.util.sendLang

object CommandProfile {

    @CommandBody
    val level = Level

    @CommandBody
    val experience = Experience

    @CommandBody(aliases = ["mp"])
    val magicpoint = MagicPoint


    fun process(func: (Player, Int) -> Unit) = subCommand {
        dynamic("player") {
            suggestPlayers()
            dynamic("value") {
                execute<ProxyCommandSender> { _, ctx, argument ->
                    val player = ctx.player("player").castSafely<Player>() ?: return@execute
                    func(player, argument.cint)
                }
            }
        }
    }

    object MagicPoint {

        // template magicpoint add <player> <value>
        @CommandBody
        val add = process { player, i ->
            PlayerTemplateAPI.addMagicPoint(player, i)
            player.sendLang("command-magicpoint-add", player.name, i)
        }

        // template magicpoint take <player> <value>
        @CommandBody
        val take = process { player, i ->
            PlayerTemplateAPI.takeMagicPoint(player, i)
            player.sendLang("command-magicpoint-take", player.name, i)
        }

        // template magicpoint set <player> <value>
        @CommandBody
        val set = process { player, i ->
            PlayerTemplateAPI.setMagicPoint(player, i)
            player.sendLang("command-magicpoint-set", player.name, i)
        }

        // template magicpoint reset <player>
        @CommandBody
        val reset = subCommand {
            dynamic("player") {
                suggestPlayers()
                execute<ProxyCommandSender> { _, ctx, argument ->
                    val player = ctx.player("player").castSafely<Player>() ?: return@execute
                    PlayerTemplateAPI.resetMagicPoint(player)
                    player.sendLang("command-magicpoint-reset", player.name)
                }
            }
        }


    }

    object Experience {

        // template experience add <player> <value>
        @CommandBody
        val add = process { player, i ->
            PlayerTemplateAPI.addExperience(player, i)
            player.sendLang("command-experience-add", player.name, i)
        }

        // template experience take <player> <value>
        @CommandBody
        val take = process { player, i ->
            PlayerTemplateAPI.takeExperience(player, i)
            player.sendLang("command-experience-take", player.name, i)
        }

        // template experience set <player> <value>
        @CommandBody
        val set = process { player, i ->
            PlayerTemplateAPI.setExperience(player, i)
            player.sendLang("command-experience-set", player.name, i)
        }

    }

    object Level {

        // template level add <player> <value>
        @CommandBody
        val add = process { player, i ->
            PlayerTemplateAPI.addLevel(player, i)
            player.sendLang("command-level-add", player.name, i)
        }

        // template level take <player> <value>
        @CommandBody
        val take = process { player, i ->
            PlayerTemplateAPI.addLevel(player, -i)
            player.sendLang("command-level-take", player.name, i)
        }

        // template level set <player> <value>
        @CommandBody
        val set = process { player, i ->
            PlayerTemplateAPI.setLevel(player, i)
            player.sendLang("command-level-set", player.name, i)
        }
    }

}
