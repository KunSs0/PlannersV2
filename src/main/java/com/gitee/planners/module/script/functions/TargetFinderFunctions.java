package com.gitee.planners.module.script.functions;

import com.gitee.planners.module.fluxon.finder.TargetFinder;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;
import com.gitee.planners.module.script.ScriptContext;

import org.bukkit.Location;
import com.gitee.planners.api.job.target.ProxyTarget;
import org.bukkit.entity.LivingEntity;

/**
 * TargetFinder 入口函数
 * <p>
 * 链式方法 (range, type, limit, build 等) 由 {@link TargetFinder} Java 对象直接提供。
 * <pre>{@code
 * // JS: finder().range(10).type("ZOMBIE").limit(3).build()
 * // JS: finder(location).range(5).build()
 * }</pre>
 */
public final class TargetFinderFunctions {

    private TargetFinderFunctions() {}

    public static void register() {
        // finder() 或 finder(origin)
        GlobalFunctions.register("finder", args -> {
            Object originArg = ScriptArgs.get(args, 0);
            Location origin = resolveLocation(originArg);
            Object senderObj = ScriptContext.getSender();
            LivingEntity sender = senderObj instanceof LivingEntity ? (LivingEntity) senderObj : null;
            if (origin == null) {
                if (sender != null) {
                    origin = sender.getLocation();
                } else {
                    throw new IllegalStateException("Cannot resolve origin: pass a Location or ensure sender is a LivingEntity");
                }
            }
            return new TargetFinder(origin, sender);
        });
    }

    private static Location resolveLocation(Object arg) {
        if (arg instanceof Location) {
            return (Location) arg;
        }
        if (arg instanceof ProxyTarget.BukkitLocation) {
            return ((ProxyTarget.BukkitLocation) arg).getBukkitLocation();
        }
        if (arg instanceof ProxyTarget.BukkitEntity) {
            return ((ProxyTarget.BukkitEntity) arg).getBukkitLocation();
        }
        if (arg instanceof LivingEntity) {
            return ((LivingEntity) arg).getLocation();
        }
        return null;
    }
}
