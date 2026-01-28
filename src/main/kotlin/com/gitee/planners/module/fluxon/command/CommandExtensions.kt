package com.gitee.planners.module.fluxon.command

import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.fluxon.FluxonScriptCache

import com.gitee.planners.module.fluxon.getTargetsArg
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 命令执行扩展
 */
object CommandExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // command(cmd) - 以 sender 身份执行命令
        runtime.registerFunction("command", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val cmd = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.CommandSender<*>>().forEach { target ->
                target.dispatchCommand(cmd)
            }
            null
        }

        // command(cmd, targets) - 以目标身份执行命令
        runtime.registerFunction("command", returns(Type.VOID).params(Type.STRING, Type.OBJECT)) { ctx ->
            val cmd = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            targets.filterIsInstance<ProxyTarget.CommandSender<*>>().forEach { target ->
                target.dispatchCommand(cmd)
            }
            null
        }

        // commandOp(cmd) - 以 OP 权限执行命令
        runtime.registerFunction("commandOp", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val cmd = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            executeAsOp(cmd, targets)
            null
        }

        // commandOp(cmd, targets) - 以 OP 权限执行命令
        runtime.registerFunction("commandOp", returns(Type.VOID).params(Type.STRING, Type.OBJECT)) { ctx ->
            val cmd = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            executeAsOp(cmd, targets)
            null
        }

        // commandConsole(cmd) - 以控制台身份执行命令
        runtime.registerFunction("commandConsole", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val cmd = ctx.getString(0) ?: return@registerFunction
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
            null
        }
    }

    private fun executeAsOp(cmd: String, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
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
    }
}
