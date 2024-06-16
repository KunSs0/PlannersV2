package com.gitee.planners.core.command

import com.gitee.planners.api.common.Unique
import org.bukkit.entity.Player
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.*

fun with(block: ProxyCommandSender.(player: Player) -> Unit) = subCommand {
    dynamic("player") {
        suggestPlayers()
        execute<ProxyCommandSender> { sender, ctx, _ ->
            val player = ctx.player("player").castSafely<Player>() ?: return@execute
            block(sender, player)
        }
    }
}

fun withList(comment: String,suggest: List<String>,block: ProxyCommandSender.(player: Player,value: String) -> Unit): SimpleCommandBody {
    return subCommand {
        dynamic("player") {
            suggestPlayers()
            dynamic(comment) {
                suggest { suggest }
                execute<ProxyCommandSender>{sender, ctx, _ ->
                    val player = ctx.player("player").castSafely<Player>() ?: return@execute
                    block(sender, player,ctx.self())
                }
            }

        }
    }
}

fun <T : Unique> withUnique(comment: String, suggest: List<T>, block: ProxyCommandSender.(player: Player, element: T) -> Unit): SimpleCommandBody {
    return withUnique(comment,{ suggest },block)
}

fun <T: Unique> withUnique(comment: String, onSuggestion: ProxyCommandSender.(Player) -> List<T>, block: ProxyCommandSender.(player: Player, element: T) -> Unit): SimpleCommandBody {
    return subCommand {
        dynamic("player") {
            suggestPlayers()
            dynamic(comment) {
                suggestion<ProxyCommandSender>{sender, ctx ->
                    onSuggestion(sender,ctx.player("player").castSafely<Player>()!!).map { it.id }
                }
                execute<ProxyCommandSender>{sender, ctx, _ ->
                    val player = ctx.player("player").castSafely<Player>() ?: return@execute
                    val map = onSuggestion(sender, ctx.player("player").castSafely<Player>()!!)
                    block(sender, player,map.first { it.id == ctx.self() })
                }
            }

        }
    }
}

fun CommandContext<*>.getBukkitPlayer(): Player? {
    return player("player").castSafely<Player>()
}
