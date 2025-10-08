package com.gitee.planners.core.command

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.Registries
import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerSkill
import org.bukkit.entity.Player
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.*
import taboolib.common.util.asList
import taboolib.common5.cint
import taboolib.common5.clong
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
    val metadata = CommandMetadata

    @CommandBody
    val profile = CommandProfile

    @CommandBody
    val state = CommandState

    @CommandBody
    val test = subCommand {
        dynamic("state") {
            suggest { Registries.STATE.keys().toList() }

            dynamic("duration") {
                suggest { listOf(1, 5, 10, 20, 30, 60, 120, 180, 240, 300, 600, 1200, 1800, 2400, 3000, 6000, 12000, 18000, 24000, 30000).asList() }

                execute<Player> { player, context, argument ->
                    val state = Registries.STATE.getOrNull(context["state"])
                    if (state == null) {
                        player.sendMessage("State '$argument' not found.")
                        return@execute
                    }
                    val duration = argument.clong
                    // 测试添加状态
                    player.adaptTarget().addState(state,duration, true)
                    player.sendMessage("Test.")
                }
            }
        }
    }

    @CommandBody
    val reload = subCommand {
        execute<ProxyCommandSender> { sender, context, argument ->
            PluginReloadEvents.Pre().call()
            Registries.handleReload()
            PluginReloadEvents.Post().call()
            sender.sendMessage("Reloaded.")
        }
    }

    @CommandBody
    val console = CommandConsole

    fun withImmutableSkill(block: ProxyCommandSender.(player: Player, skill: ImmutableSkill) -> Unit): SimpleCommandBody {
        return withUnique("id", { Registries.SKILL.values().toList() }, block)
    }

    fun withPlayerSkill(block: ProxyCommandSender.(player: Player, skill: PlayerSkill) -> Unit): SimpleCommandBody {
        return withUnique("id", { it.plannersTemplate.getRegisteredSkill().values.toList() }, block)
    }
}
