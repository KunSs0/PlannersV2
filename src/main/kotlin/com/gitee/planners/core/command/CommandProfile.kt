package com.gitee.planners.core.command

import com.gitee.planners.api.ProfileAPI
import com.gitee.planners.api.ProfileAPI.plannersProfile
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

        // profile magicpoint add <player> <value>
        @CommandBody
        val add = process { player, i ->
            ProfileAPI.addMagicPoint(player, i)
            player.sendLang("command-magicpoint-add", player.name, i)
        }

        // profile magicpoint take <player> <value>
        @CommandBody
        val take = process { player, i ->
            ProfileAPI.takeMagicPoint(player, i)
            player.sendLang("command-magicpoint-take", player.name, i)
        }

        // profile magicpoint set <player> <value>
        @CommandBody
        val set = process { player, i ->
            ProfileAPI.setMagicPoint(player, i)
            player.sendLang("command-magicpoint-set", player.name, i)
        }

        // profile magicpoint reset <player>
        @CommandBody
        val reset = subCommand {
            dynamic("player") {
                suggestPlayers()
                execute<ProxyCommandSender> { _, ctx, argument ->
                    val player = ctx.player("player").castSafely<Player>() ?: return@execute
                    ProfileAPI.resetMagicPoint(player)
                    player.sendLang("command-magicpoint-reset", player.name)
                }
            }
        }


    }

    object Experience {

        // profile experience add <player> <value>
        @CommandBody
        val add = process { player, i ->
            ProfileAPI.addExperience(player, i)
            player.sendLang("command-experience-add", player.name, i)
        }

        // profile experience take <player> <value>
        @CommandBody
        val take = process { player, i ->
            ProfileAPI.takeExperience(player, i)
            player.sendLang("command-experience-take", player.name, i)
        }

        // profile experience set <player> <value>
        @CommandBody
        val set = process { player, i ->
            ProfileAPI.setExperience(player, i)
            player.sendLang("command-experience-set", player.name, i)
        }

    }

    object Level {

        // profile level add <player> <value>
        @CommandBody
        val add = process { player, i ->
            ProfileAPI.addLevel(player, i)
            player.sendLang("command-level-add", player.name, i)
        }

        // profile level take <player> <value>
        @CommandBody
        val take = process { player, i ->
            ProfileAPI.addLevel(player, -i)
            player.sendLang("command-level-take", player.name, i)
        }

        // profile level set <player> <value>
        @CommandBody
        val set = process { player, i ->
            ProfileAPI.setLevel(player, i)
            player.sendLang("command-level-set", player.name, i)
        }
    }

}
