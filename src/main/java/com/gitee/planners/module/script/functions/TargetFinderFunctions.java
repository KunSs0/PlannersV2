package com.gitee.planners.module.script.functions;

import com.gitee.planners.api.common.facing.EntityFacingProviders;
import com.gitee.planners.module.script.finder.TargetFinder;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;
import com.gitee.planners.module.script.ScriptContext;

import org.bukkit.Location;
import com.gitee.planners.api.job.target.ProxyTarget;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

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
            LivingEntity originEntity = resolveLivingEntity(originArg);
            Object senderObj = ScriptContext.getSender();
            LivingEntity sender = resolveLivingEntity(senderObj);
            if (origin == null) {
                Map<String, Object> context = ScriptContext.getCurrent();
                if (context != null) {
                    Object contextOrigin = context.get("origin");
                    origin = resolveLocation(contextOrigin);
                    originEntity = resolveLivingEntity(contextOrigin);
                }
            }
            if (origin == null) {
                if (sender != null) {
                    origin = sender.getLocation();
                    originEntity = sender;
                } else {
                    throw new IllegalStateException("Cannot resolve origin: pass a Location, set context origin, or ensure sender is a LivingEntity");
                }
            }
            Float facingYaw = null;
            if (originEntity != null) {
                facingYaw = EntityFacingProviders.getFacingYaw(originEntity);
            } else if (sender != null) {
                facingYaw = EntityFacingProviders.getFacingYaw(sender);
            }
            return new TargetFinder(origin, sender, facingYaw);
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

    private static LivingEntity resolveLivingEntity(Object arg) {
        if (arg instanceof LivingEntity) {
            return (LivingEntity) arg;
        }
        if (arg instanceof ProxyTarget.BukkitEntity) {
            Entity entity = ((ProxyTarget.BukkitEntity) arg).getInstance();
            return entity instanceof LivingEntity ? (LivingEntity) entity : null;
        }
        return null;
    }
}
