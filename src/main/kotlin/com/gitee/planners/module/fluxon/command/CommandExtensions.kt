package com.gitee.planners.module.fluxon.command

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.getTargetsArg
import com.gitee.planners.module.fluxon.registerFunction
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 命令执行扩展
 */
object CommandExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // command(cmd, [targets]) - 以目标身份执行命令
        runtime.registerFunction("command", listOf(1, 2)) { ctx ->
            val cmd = ctx.getAsString(0) ?: return@registerFunction null
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.CommandSender<*>>().forEach { target ->
                target.dispatchCommand(cmd)
            }
            null
        }

        // commandOp(cmd, [targets]) - 以 OP 权限执行命令
        runtime.registerFunction("commandOp", listOf(1, 2)) { ctx ->
            val cmd = ctx.getAsString(0) ?: return@registerFunction null
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                val player = target.instance as? Player ?: return@forEach
                val wasOp = player.isOp
                try {
                    player.isOp = true
                    Bukkit.dispatchCommand(player, cmd)
                } finally {
                    player.isOp = wasOp
                }
            }
            null
        }

        // commandConsole(cmd) - 以控制台身份执行命令
        runtime.registerFunction("commandConsole", listOf(1)) { ctx ->
            val cmd = ctx.getAsString(0) ?: return@registerFunction null
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
            null
        }
    }
}
