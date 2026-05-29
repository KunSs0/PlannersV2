package com.gitee.planners.module.script.functions;

import com.gitee.planners.api.job.target.LeastType;
import com.gitee.planners.api.job.target.ProxyTarget;
import com.gitee.planners.api.job.target.ProxyTargetContainer;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;

import org.bukkit.Bukkit;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 通用扩展函数
 * <pre>{@code
 * // JS: tell("hello")           — 发送消息给 sender
 * // JS: tell("hello", targets)  — 发送消息给目标
 * }</pre>
 */
public final class CommonFunctions {

    private CommonFunctions() {}

    public static void register() {
        // random(min, max) — 返回 [min, max] 之间的随机整数
        GlobalFunctions.register("random", args -> {
            int min = ScriptArgs.getInt(args, 0);
            int max = ScriptArgs.getInt(args, 1);
            return ThreadLocalRandom.current().nextInt(min, max + 1);
        });

        // sleep(millis) — 暂停当前脚本执行（仅异步上下文可用）
        GlobalFunctions.register("sleep", args -> {
            long millis = ScriptArgs.getLong(args, 0);
            if (Bukkit.isPrimaryThread()) {
                throw new IllegalStateException("sleep() 不能在主线程调用，请确保技能配置 async: true");
            }
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        });

        // tell(message) 或 tell(message, targets)
        GlobalFunctions.register("tell", args -> {
            String message = ScriptArgs.getString(args, 0);
            if (message == null) return null;
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            for (ProxyTarget<?> t : targets) {
                if (t instanceof ProxyTarget.CommandSender) {
                    ((ProxyTarget.CommandSender<?>) t).sendMessage(message);
                }
            }
            return null;
        });
    }
}
