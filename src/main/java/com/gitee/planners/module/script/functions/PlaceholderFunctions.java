package com.gitee.planners.module.script.functions;

import com.gitee.planners.api.job.target.ProxyTarget;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;
import com.gitee.planners.module.script.ScriptContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import taboolib.platform.BukkitPlugin;
import taboolib.platform.compat.PlaceholderExpansionKt;

import java.util.concurrent.Callable;

/**
 * PlaceholderAPI 脚本函数。
 * <p>
 * 将 PlaceholderAPI 的占位符文本解析为字符串。未指定玩家时使用当前脚本的施法者，
 * 调用方必须保证施法者或第二个参数为 {@link Player}。
 * <pre>{@code
 * // JS: papi("%player_name%")
 * // JS: papi("%player_name%", player)
 * }</pre>
 */
public final class PlaceholderFunctions {

    private PlaceholderFunctions() {
    }

    /**
     * 注册 PlaceholderAPI 相关的全局脚本函数。
     */
    public static void register() {
        GlobalFunctions.register("papi", args -> {
            String placeholder = ScriptArgs.getString(args, 0);
            if (placeholder == null || placeholder.isEmpty()) {
                throw new IllegalArgumentException("papi() 的第一个参数必须是非空占位符文本");
            }

            Player player = resolvePlayer(args);
            if (player == null) {
                throw new IllegalStateException("papi() 需要玩家施法者或第二个 Player 参数");
            }

            requirePlaceholderApi();
            return callOnMainThread(() -> PlaceholderExpansionKt.replacePlaceholder(placeholder, player));
        });
    }

    /**
     * 从第二个参数或当前脚本施法者解析玩家。
     *
     * @param args JavaScript 调用传入的参数。
     * @return 可用的 Bukkit 玩家；无法解析时返回 {@code null}。
     */
    private static Player resolvePlayer(Object[] args) {
        Object candidate = ScriptArgs.get(args, 1);
        if (candidate == null) {
            candidate = ScriptContext.getSender();
        }
        if (candidate instanceof Player) {
            return (Player) candidate;
        }
        if (candidate instanceof ProxyTarget.BukkitEntity) {
            Object instance = ((ProxyTarget.BukkitEntity) candidate).getInstance();
            if (instance instanceof Player) {
                return (Player) instance;
            }
        }
        return null;
    }

    /**
     * 确保服务器已启用 PlaceholderAPI。
     */
    private static void requirePlaceholderApi() {
        Plugin placeholderApi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (placeholderApi == null || !placeholderApi.isEnabled()) {
            throw new IllegalStateException("papi() 需要已启用的 PlaceholderAPI 插件");
        }
    }

    /**
     * 在 Bukkit 主线程执行 PlaceholderAPI 调用。
     *
     * @param callable 主线程执行的解析操作。
     * @return 占位符解析结果。
     */
    private static String callOnMainThread(Callable<String> callable) {
        if (Bukkit.isPrimaryThread()) {
            try {
                return callable.call();
            } catch (Exception exception) {
                throw new RuntimeException("PlaceholderAPI 占位符解析失败", exception);
            }
        }
        try {
            return Bukkit.getScheduler().callSyncMethod(BukkitPlugin.getInstance(), callable).get();
        } catch (Exception exception) {
            throw new RuntimeException("PlaceholderAPI 占位符解析失败", exception);
        }
    }
}
