package com.gitee.planners.core.command

import com.gitee.planners.api.ProfileAPI.plannersProfile
import com.gitee.planners.api.RegistryBuiltin
import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerSkill
import org.bukkit.entity.Player
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.*
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
    val reload = subCommand {
        execute<ProxyCommandSender> { sender, context, argument ->
            PluginReloadEvents.Pre().call()
            RegistryBuiltin.handleReload()
            PluginReloadEvents.Post().call()
            sender.sendMessage("Reloaded.")
        }
    }

    @CommandBody
    val console = CommandConsole

    fun withImmutableSkill(block: ProxyCommandSender.(player: Player, skill: ImmutableSkill) -> Unit): SimpleCommandBody {
        return withUnique("id", { RegistryBuiltin.SKILL.getValues() }, block)
    }

    fun withPlayerSkill(block: ProxyCommandSender.(player: Player, skill: PlayerSkill) -> Unit): SimpleCommandBody {
        return withUnique("id", { it.plannersProfile.getRegistrySkill().getValues() }, block)
    }
}
