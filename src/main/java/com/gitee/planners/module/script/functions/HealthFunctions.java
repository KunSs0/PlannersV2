package com.gitee.planners.module.script.functions;

import com.gitee.planners.api.job.target.LeastType;
import com.gitee.planners.api.job.target.ProxyTarget;
import com.gitee.planners.api.job.target.ProxyTargetContainer;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;
import org.bukkit.entity.LivingEntity;

/**
 * 生命值操作全局函数
 * <p>
 * 迁移自 {@code HealthExtensions.kt}，合并同名重载。
 * <pre>{@code
 * // JS: healthAdd(50)  healthAdd(50, targets)
 * // JS: healthSet(100)  healthSet(100, targets)
 * // JS: healthTake(30)  healthTake(30, targets)
 * }</pre>
 */
public final class HealthFunctions {

    private HealthFunctions() {}

    @SuppressWarnings("deprecation")
    public static void register() {
        // healthAdd(amount) / healthAdd(amount, targets)
        GlobalFunctions.register("healthAdd", args -> {
            double amount = ScriptArgs.getDouble(args, 0);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            forEachLiving(targets, entity -> {
                entity.setHealth(Math.min(entity.getHealth() + amount, entity.getMaxHealth()));
            });
            return null;
        });

        // healthSet(amount) / healthSet(amount, targets)
        GlobalFunctions.register("healthSet", args -> {
            double amount = ScriptArgs.getDouble(args, 0);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            forEachLiving(targets, entity -> {
                entity.setHealth(Math.max(0.0, Math.min(amount, entity.getMaxHealth())));
            });
            return null;
        });

        // healthTake(amount) / healthTake(amount, targets)
        GlobalFunctions.register("healthTake", args -> {
            double amount = ScriptArgs.getDouble(args, 0);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            forEachLiving(targets, entity -> {
                entity.setHealth(Math.max(entity.getHealth() - amount, 0.0));
            });
            return null;
        });
    }

    private static void forEachLiving(ProxyTargetContainer targets, java.util.function.Consumer<LivingEntity> action) {
        for (ProxyTarget<?> t : targets) {
            if (!(t instanceof ProxyTarget.BukkitEntity)) continue;
            Object instance = ((ProxyTarget.BukkitEntity) t).getInstance();
            if (!(instance instanceof LivingEntity)) continue;
            action.accept((LivingEntity) instance);
        }
    }
}
