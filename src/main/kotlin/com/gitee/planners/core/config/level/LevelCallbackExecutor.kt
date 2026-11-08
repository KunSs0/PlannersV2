package com.gitee.planners.core.config.level

import com.gitee.planners.api.event.player.PlayerLevelChangeEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.warning

object LevelCallbackExecutor {

    @SubscribeEvent(ignoreCancelled = true)
    fun e(e: PlayerLevelChangeEvent) {
        if (e.to <= e.form) {
            return
        }
        var algorithm = e.template.playerRouter?.algorithm
        if (algorithm == null) {
            algorithm = AlgorithmLevel.default
        }
        if (algorithm == null) {
            return
        }
        for (level in (e.form + 1)..e.to) {
            val callbacks = algorithm.getCallbacks(level)
            if (callbacks.isEmpty()) {
                continue
            }
            if (Bukkit.isPrimaryThread()) {
                runCallbacks(e, level)
            } else {
                submit { runCallbacks(e, level) }
            }
        }
    }

    private fun runCallbacks(e: PlayerLevelChangeEvent, level: Int) {
        val player = e.player
        var algorithm = e.template.playerRouter?.algorithm
        if (algorithm == null) {
            algorithm = AlgorithmLevel.default
        }
        if (algorithm == null) {
            return
        }
        for (callback in algorithm.getCallbacks(level)) {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replacePlaceholders(callback.command, player, level, e))
            } catch (ex: Throwable) {
                warning("等级回调执行失败: player=${player.name}, level=$level, command=${callback.command}", ex)
            }
        }
    }

    private fun replacePlaceholders(
        command: String,
        player: Player,
        level: Int,
        e: PlayerLevelChangeEvent
    ): String {
        return command
            .replace("{player}", player.name)
            .replace("{playerName}", player.name)
            .replace("{name}", player.name)
            .replace("{uuid}", player.uniqueId.toString())
            .replace("{level}", level.toString())
            .replace("{from}", e.form.toString())
            .replace("{to}", e.to.toString())
    }
}
