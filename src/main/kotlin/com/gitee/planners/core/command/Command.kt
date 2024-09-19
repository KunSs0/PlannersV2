package com.gitee.planners.core.command

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.Registries
import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.module.kether.selector.RectangleBody
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
    val profile = CommandProfile

    @CommandBody
    val test = subCommand {
        execute<Player> { player, context, argument ->
//            val shapeBlock = RectangleBody.ShapeBlock(10.0, 5.0, 2.0)
//            shapeBlock.drawTest(player)
//            shapeBlock.find(player)

            player.sendMessage("Test.")
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
