package com.gitee.planners.module.script.functions;

import com.gitee.planners.api.job.target.LeastType;
import com.gitee.planners.api.job.target.ProxyTarget;
import com.gitee.planners.api.job.target.ProxyTargetContainer;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import taboolib.platform.BukkitPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体生成与控制全局函数
 * <p>
 * 迁移自 {@code EntityExtensions.kt}，合并同名重载。
 * <pre>{@code
 * // JS: entitySpawn("ZOMBIE")  entitySpawn("ZOMBIE", 100)  entitySpawn("ZOMBIE", 100, locations)
 * // JS: entityRemove()  entityRemove(targets)
 * // JS: entityTeleport(x, y, z)  entityTeleport(x, y, z, targets)
 * // JS: entityTeleportTo(destinations)  entityTeleportTo(targets, destinations)
 * // JS: entitySetAI(true)  entitySetAI(true, targets)
 * // JS: entitySetGravity / entitySetInvulnerable / entitySetGlowing / entitySetSilent
 * }</pre>
 */
public final class EntityFunctions {

    private EntityFunctions() {}

    public static void register() {
        // entitySpawn(type) / entitySpawn(type, duration) / entitySpawn(type, duration, locations)
        GlobalFunctions.register("entitySpawn", args -> {
            String typeName = ScriptArgs.getString(args, 0);
            if (typeName == null) return null;
            long duration = args.length >= 2 ? ScriptArgs.getLong(args, 1) : -1L;
            ProxyTargetContainer locations = ScriptArgs.getTargets(args, 2, LeastType.ORIGIN);
            return spawnEntities(typeName, duration, locations);
        });

        // entityRemove() / entityRemove(targets)
        GlobalFunctions.register("entityRemove", args -> {
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 0, LeastType.EMPTY);
            for (ProxyTarget<?> t : targets) {
                if (t instanceof ProxyTarget.BukkitEntity) {
                    Entity entity = ((ProxyTarget.BukkitEntity) t).getInstance();
                    if (entity.isValid()) {
                        entity.remove();
                    }
                }
            }
            return null;
        });

        // entityTeleport(x, y, z) / entityTeleport(x, y, z, targets)
        GlobalFunctions.register("entityTeleport", args -> {
            double x = ScriptArgs.getDouble(args, 0);
            double y = ScriptArgs.getDouble(args, 1);
            double z = ScriptArgs.getDouble(args, 2);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 3, LeastType.SENDER);
            for (ProxyTarget<?> t : targets) {
                if (t instanceof ProxyTarget.BukkitEntity) {
                    Entity entity = ((ProxyTarget.BukkitEntity) t).getInstance();
                    Location loc = entity.getLocation().clone();
                    loc.setX(x);
                    loc.setY(y);
                    loc.setZ(z);
                    entity.teleport(loc);
                }
            }
            return null;
        });

        // entityTeleportTo(destinations) / entityTeleportTo(targets, destinations)
        GlobalFunctions.register("entityTeleportTo", args -> {
            ProxyTargetContainer targets;
            ProxyTargetContainer destinations;
            if (args.length >= 2) {
                targets = ScriptArgs.getTargets(args, 0, LeastType.SENDER);
                destinations = ScriptArgs.getTargets(args, 1, LeastType.EMPTY);
            } else {
                targets = ScriptArgs.getTargets(args, -1, LeastType.SENDER);
                destinations = ScriptArgs.getTargets(args, 0, LeastType.EMPTY);
            }
            Location dest = null;
            for (ProxyTarget<?> d : destinations) {
                if (d instanceof ProxyTarget.Location) {
                    dest = ((ProxyTarget.Location<?>) d).getBukkitLocation();
                    break;
                }
            }
            if (dest == null) return null;
            for (ProxyTarget<?> t : targets) {
                if (t instanceof ProxyTarget.BukkitEntity) {
                    ((ProxyTarget.BukkitEntity) t).getInstance().teleport(dest);
                }
            }
            return null;
        });

        // entitySetAI(enabled) / entitySetAI(enabled, targets)
        GlobalFunctions.register("entitySetAI", args -> {
            boolean enabled = ScriptArgs.getBoolean(args, 0);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            for (ProxyTarget<?> t : targets) {
                if (t instanceof ProxyTarget.BukkitEntity) {
                    Entity entity = ((ProxyTarget.BukkitEntity) t).getInstance();
                    if (entity instanceof LivingEntity) {
                        ((LivingEntity) entity).setAI(enabled);
                    }
                }
            }
            return null;
        });

        // entitySetGravity(enabled) / entitySetGravity(enabled, targets)
        GlobalFunctions.register("entitySetGravity", args -> {
            boolean enabled = ScriptArgs.getBoolean(args, 0);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            forEachEntity(targets, entity -> entity.setGravity(enabled));
            return null;
        });

        // entitySetInvulnerable(enabled) / entitySetInvulnerable(enabled, targets)
        GlobalFunctions.register("entitySetInvulnerable", args -> {
            boolean enabled = ScriptArgs.getBoolean(args, 0);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            forEachEntity(targets, entity -> entity.setInvulnerable(enabled));
            return null;
        });

        // entitySetGlowing(enabled) / entitySetGlowing(enabled, targets)
        GlobalFunctions.register("entitySetGlowing", args -> {
            boolean enabled = ScriptArgs.getBoolean(args, 0);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            forEachEntity(targets, entity -> entity.setGlowing(enabled));
            return null;
        });

        // entitySetSilent(enabled) / entitySetSilent(enabled, targets)
        GlobalFunctions.register("entitySetSilent", args -> {
            boolean enabled = ScriptArgs.getBoolean(args, 0);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            forEachEntity(targets, entity -> entity.setSilent(enabled));
            return null;
        });
    }

    private static Object spawnEntities(String typeName, long duration, ProxyTargetContainer locations) {
        EntityType type;
        try {
            type = EntityType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
        List<ProxyTarget<?>> entities = new ArrayList<>();
        for (ProxyTarget<?> loc : locations) {
            if (!(loc instanceof ProxyTarget.Location)) continue;
            Location bukLoc = ((ProxyTarget.Location<?>) loc).getBukkitLocation();
            if (bukLoc.getWorld() == null) continue;
            Entity entity = bukLoc.getWorld().spawnEntity(bukLoc, type);
            if (duration > 0) {
                Bukkit.getScheduler().runTaskLater(BukkitPlugin.getInstance(), () -> {
                    if (entity.isValid()) {
                        entity.remove();
                    }
                }, duration);
            }
            entities.add(ProxyTarget.Companion.of(entity));
        }
        return entities.size() == 1 ? entities.get(0) : entities;
    }

    private static void forEachEntity(ProxyTargetContainer targets, java.util.function.Consumer<Entity> action) {
        for (ProxyTarget<?> t : targets) {
            if (t instanceof ProxyTarget.BukkitEntity) {
                action.accept(((ProxyTarget.BukkitEntity) t).getInstance());
            }
        }
    }
}
