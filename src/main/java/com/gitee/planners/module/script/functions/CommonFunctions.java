package com.gitee.planners.module.script.functions;

import com.gitee.planners.api.job.target.LeastType;
import com.gitee.planners.api.job.target.ProxyTarget;
import com.gitee.planners.api.job.target.ProxyTargetContainer;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;

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
