package com.gitee.planners.module.script.functions;

import com.gitee.planners.api.job.target.LeastType;
import com.gitee.planners.api.job.target.ProxyTarget;
import com.gitee.planners.api.job.target.ProxyTargetContainer;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;
import org.bukkit.Location;

/**
 * 实体状态效果全局函数
 * <p>
 * 迁移自 {@code EffectExtensions.kt}，合并同名重载。
 * <pre>{@code
 * // JS: freeze(ticks)  freeze(ticks, targets)
 * // JS: fire(ticks)  fire(ticks, targets)
 * // JS: explosion(power)  explosion(power, fire, breakBlocks, targets)
 * }</pre>
 */
public final class EffectFunctions {

    private EffectFunctions() {}

    public static void register() {
        // freeze(ticks) / freeze(ticks, targets)
        GlobalFunctions.register("freeze", args -> {
            int ticks = ScriptArgs.getInt(args, 0);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            for (ProxyTarget<?> t : targets) {
                if (t instanceof ProxyTarget.BukkitEntity) {
                    ((ProxyTarget.BukkitEntity) t).getInstance().setFreezeTicks(ticks);
                }
            }
            return null;
        });

        // fire(ticks) / fire(ticks, targets)
        GlobalFunctions.register("fire", args -> {
            int ticks = ScriptArgs.getInt(args, 0);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            for (ProxyTarget<?> t : targets) {
                if (t instanceof ProxyTarget.BukkitEntity) {
                    ((ProxyTarget.BukkitEntity) t).getInstance().setFireTicks(ticks);
                }
            }
            return null;
        });

        // explosion(power) / explosion(power, fire) / explosion(power, fire, breakBlocks) / explosion(power, fire, breakBlocks, targets)
        GlobalFunctions.register("explosion", args -> {
            float power = ScriptArgs.getFloat(args, 0);
            boolean setFire = ScriptArgs.getBoolean(args, 1);
            boolean breakBlocks = ScriptArgs.getBoolean(args, 2);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 3, LeastType.ORIGIN);
            for (ProxyTarget<?> t : targets) {
                if (t instanceof ProxyTarget.Location) {
                    Location loc = ((ProxyTarget.Location<?>) t).getBukkitLocation();
                    if (loc.getWorld() != null) {
                        loc.getWorld().createExplosion(loc, power, setFire, breakBlocks);
                    }
                }
            }
            return null;
        });
    }
}
