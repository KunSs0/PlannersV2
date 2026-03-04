package com.gitee.planners.module.script.functions;

import com.gitee.planners.api.job.target.LeastType;
import com.gitee.planners.api.job.target.ProxyTarget;
import com.gitee.planners.api.job.target.ProxyTargetContainer;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * 命令执行扩展函数
 * <pre>{@code
 * // JS: command("give Steve diamond 1")             — sender 身份执行
 * // JS: command("give Steve diamond 1", targets)     — 目标身份执行
 * // JS: commandOp("give Steve diamond 1")            — OP 权限执行
 * // JS: commandOp("give Steve diamond 1", targets)   — 目标以 OP 权限执行
 * // JS: commandConsole("give Steve diamond 1")       — 控制台执行
 * }</pre>
 */
public final class CommandFunctions {

    private CommandFunctions() {}

    public static void register() {
        // command(cmd) 或 command(cmd, targets)
        GlobalFunctions.register("command", args -> {
            String cmd = ScriptArgs.getString(args, 0);
            if (cmd == null) return null;
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            for (ProxyTarget<?> t : targets) {
                if (t instanceof ProxyTarget.CommandSender) {
                    ((ProxyTarget.CommandSender<?>) t).dispatchCommand(cmd);
                }
            }
            return null;
        });

        // commandOp(cmd) 或 commandOp(cmd, targets)
        GlobalFunctions.register("commandOp", args -> {
            String cmd = ScriptArgs.getString(args, 0);
            if (cmd == null) return null;
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            executeAsOp(cmd, targets);
            return null;
        });

        // commandConsole(cmd)
        GlobalFunctions.register("commandConsole", args -> {
            String cmd = ScriptArgs.getString(args, 0);
            if (cmd == null) return null;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            return null;
        });
    }

    private static void executeAsOp(String cmd, ProxyTargetContainer targets) {
        for (ProxyTarget<?> t : targets) {
            if (t instanceof ProxyTarget.BukkitEntity) {
                Object instance = ((ProxyTarget.BukkitEntity) t).getInstance();
                if (instance instanceof Player) {
                    Player player = (Player) instance;
                    boolean wasOp = player.isOp();
                    try {
                        player.setOp(true);
                        Bukkit.dispatchCommand(player, cmd);
                    } finally {
                        player.setOp(wasOp);
                    }
                }
            }
        }
    }
}
