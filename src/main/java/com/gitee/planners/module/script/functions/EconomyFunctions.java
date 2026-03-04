package com.gitee.planners.module.script.functions;

import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * 经济系统函数 (Vault Economy)
 * <p>
 * 迁移自 {@code EconomyExtensions.kt}
 * <pre>{@code
 * // JS: getBalance()  or  getBalance(player)
 * // JS: takeMoney(100)  or  takeMoney(100, player)
 * // JS: giveMoney(100)  or  giveMoney(100, player)
 * // JS: setMoney(100)  or  setMoney(100, player)
 * }</pre>
 */
public final class EconomyFunctions {

    private EconomyFunctions() {}

    private static Economy getEconomy() {
        try {
            var reg = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
            return reg != null ? reg.getProvider() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static void register() {
        // getBalance() 或 getBalance(player)
        GlobalFunctions.register("getBalance", args -> {
            Player player = ScriptArgs.getPlayer(args, 0);
            if (player == null) return 0.0;
            Economy eco = getEconomy();
            return eco != null ? eco.getBalance(player) : 0.0;
        });

        // takeMoney(amount) 或 takeMoney(amount, player)
        GlobalFunctions.register("takeMoney", args -> {
            double amount = ScriptArgs.getDouble(args, 0);
            Player player = ScriptArgs.getPlayer(args, 1);
            if (player == null) return false;
            Economy eco = getEconomy();
            return eco != null && eco.withdrawPlayer(player, amount).transactionSuccess();
        });

        // giveMoney(amount) 或 giveMoney(amount, player)
        GlobalFunctions.register("giveMoney", args -> {
            double amount = ScriptArgs.getDouble(args, 0);
            Player player = ScriptArgs.getPlayer(args, 1);
            if (player == null) return false;
            Economy eco = getEconomy();
            return eco != null && eco.depositPlayer(player, amount).transactionSuccess();
        });

        // setMoney(amount) 或 setMoney(amount, player)
        GlobalFunctions.register("setMoney", args -> {
            double amount = ScriptArgs.getDouble(args, 0);
            Player player = ScriptArgs.getPlayer(args, 1);
            if (player == null) return null;
            Economy eco = getEconomy();
            if (eco == null) return null;
            double current = eco.getBalance(player);
            if (current > amount) {
                eco.withdrawPlayer(player, current - amount);
            } else {
                eco.depositPlayer(player, amount - current);
            }
            return null;
        });
    }
}
