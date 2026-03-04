package com.gitee.planners.module.script.functions;

import com.gitee.planners.api.job.target.LeastType;
import com.gitee.planners.api.job.target.ProxyTarget;
import com.gitee.planners.api.job.target.ProxyTargetContainer;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;

import org.bukkit.entity.*;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * 投射物函数
 * <p>
 * 迁移自 {@code ProjectileExtensions.kt}
 * <pre>{@code
 * // JS: projectile("ARROW")                              — sender 朝向发射
 * // JS: projectile("ARROW", 2.0)                         — 指定速度
 * // JS: projectile("ARROW", 2.0, targets)                — 从目标发射
 * // JS: projectileAt("ARROW", 1, 0, 0, 2.0)             — 向指定方向
 * // JS: projectileAt("ARROW", 1, 0, 0, 2.0, targets)    — 从目标向指定方向
 * // JS: projectileToward("ARROW", 2.0)                   — 向目标位置发射
 * // JS: projectileToward("ARROW", 2.0, sources)
 * // JS: projectileToward("ARROW", 2.0, sources, dests)
 * }</pre>
 */
public final class ProjectileFunctions {

    private ProjectileFunctions() {}

    public static void register() {
        // projectile(type) / projectile(type, speed) / projectile(type, speed, targets)
        GlobalFunctions.register("projectile", args -> {
            String typeName = ScriptArgs.getString(args, 0);
            if (typeName == null) return null;
            double speed = args.length > 1 ? ScriptArgs.getDouble(args, 1) : 1.0;
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 2, LeastType.SENDER);
            return launchProjectile(typeName, speed, targets);
        });

        // projectileAt(type, x, y, z, speed) / projectileAt(type, x, y, z, speed, targets)
        GlobalFunctions.register("projectileAt", args -> {
            String typeName = ScriptArgs.getString(args, 0);
            if (typeName == null) return null;
            double x = ScriptArgs.getDouble(args, 1);
            double y = ScriptArgs.getDouble(args, 2);
            double z = ScriptArgs.getDouble(args, 3);
            double speed = ScriptArgs.getDouble(args, 4);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 5, LeastType.SENDER);
            return launchProjectileAt(typeName, new Vector(x, y, z), speed, targets);
        });

        // projectileToward(type, speed) / projectileToward(type, speed, sources) / projectileToward(type, speed, sources, dests)
        GlobalFunctions.register("projectileToward", args -> {
            String typeName = ScriptArgs.getString(args, 0);
            if (typeName == null) return null;
            double speed = ScriptArgs.getDouble(args, 1);
            ProxyTargetContainer sources = ScriptArgs.getTargets(args, 2, LeastType.SENDER);
            ProxyTargetContainer destinations = ScriptArgs.getTargets(args, 3, LeastType.EMPTY);
            return launchProjectileToward(typeName, speed, sources, destinations);
        });
    }

    private static Class<? extends Projectile> resolveProjectileType(String name) {
        switch (name.toUpperCase()) {
            case "ARROW": return Arrow.class;
            case "DRAGON_FIREBALL": return DragonFireball.class;
            case "EGG": return Egg.class;
            case "ENDER_PEARL": return EnderPearl.class;
            case "FIREBALL": return Fireball.class;
            case "LARGE_FIREBALL": return LargeFireball.class;
            case "SMALL_FIREBALL": return SmallFireball.class;
            case "SNOWBALL": return Snowball.class;
            case "SPECTRAL_ARROW": return SpectralArrow.class;
            case "TRIDENT": return Trident.class;
            case "WITHER_SKULL": return WitherSkull.class;
            case "SHULKER_BULLET": return ShulkerBullet.class;
            case "LLAMA_SPIT": return LlamaSpit.class;
            default: return null;
        }
    }

    private static List<Projectile> launchProjectile(String typeName, double speed, ProxyTargetContainer targets) {
        Class<? extends Projectile> type = resolveProjectileType(typeName);
        if (type == null) return null;
        List<Projectile> projectiles = new ArrayList<>();
        for (ProxyTarget<?> t : targets) {
            if (!(t instanceof ProxyTarget.BukkitEntity)) continue;
            Entity entity = ((ProxyTarget.BukkitEntity) t).getInstance();
            if (!(entity instanceof LivingEntity)) continue;
            LivingEntity living = (LivingEntity) entity;
            Projectile projectile = living.launchProjectile(type);
            projectile.setVelocity(projectile.getVelocity().multiply(speed));
            projectile.setMetadata("shooter", new org.bukkit.metadata.FixedMetadataValue(
                    taboolib.platform.BukkitPlugin.getInstance(), living));
            projectiles.add(projectile);
        }
        return projectiles;
    }

    private static List<Projectile> launchProjectileAt(String typeName, Vector direction, double speed, ProxyTargetContainer targets) {
        Class<? extends Projectile> type = resolveProjectileType(typeName);
        if (type == null) return null;
        Vector normalizedDirection = direction.normalize();
        List<Projectile> projectiles = new ArrayList<>();
        for (ProxyTarget<?> t : targets) {
            if (!(t instanceof ProxyTarget.BukkitEntity)) continue;
            Entity entity = ((ProxyTarget.BukkitEntity) t).getInstance();
            if (!(entity instanceof LivingEntity)) continue;
            LivingEntity living = (LivingEntity) entity;
            Projectile projectile = living.launchProjectile(type, normalizedDirection.clone().multiply(speed));
            projectile.setMetadata("shooter", new org.bukkit.metadata.FixedMetadataValue(
                    taboolib.platform.BukkitPlugin.getInstance(), living));
            projectiles.add(projectile);
        }
        return projectiles;
    }

    private static List<Projectile> launchProjectileToward(String typeName, double speed, ProxyTargetContainer sources, ProxyTargetContainer destinations) {
        Class<? extends Projectile> type = resolveProjectileType(typeName);
        if (type == null) return null;
        List<Projectile> projectiles = new ArrayList<>();
        for (ProxyTarget<?> src : sources) {
            if (!(src instanceof ProxyTarget.BukkitEntity)) continue;
            Entity srcEntity = ((ProxyTarget.BukkitEntity) src).getInstance();
            if (!(srcEntity instanceof LivingEntity)) continue;
            LivingEntity shooter = (LivingEntity) srcEntity;
            for (ProxyTarget<?> dest : destinations) {
                if (!(dest instanceof ProxyTarget.Location)) continue;
                org.bukkit.Location destLoc = ((ProxyTarget.Location<?>) dest).getBukkitLocation();
                Vector direction = destLoc.toVector()
                        .subtract(shooter.getEyeLocation().toVector())
                        .normalize()
                        .multiply(speed);
                Projectile projectile = shooter.launchProjectile(type, direction);
                projectile.setMetadata("shooter", new org.bukkit.metadata.FixedMetadataValue(
                        taboolib.platform.BukkitPlugin.getInstance(), shooter));
                projectile.setMetadata("target", new org.bukkit.metadata.FixedMetadataValue(
                        taboolib.platform.BukkitPlugin.getInstance(), destLoc));
                projectiles.add(projectile);
            }
        }
        return projectiles;
    }
}
