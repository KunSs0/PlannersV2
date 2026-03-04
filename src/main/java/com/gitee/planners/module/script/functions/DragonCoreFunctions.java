package com.gitee.planners.module.script.functions;

import com.gitee.planners.api.job.target.LeastType;
import com.gitee.planners.api.job.target.ProxyTarget;
import com.gitee.planners.api.job.target.ProxyTargetContainer;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;

import eos.moe.dragoncore.api.CoreAPI;
import eos.moe.dragoncore.network.PacketSender;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * DragonCore 集成函数
 * <p>
 * 迁移自 DragonCoreExtensions.kt，合并重载为 9 个全局函数:
 * dcParticle, dcSound, dcAnimation, dcAnimationRemove,
 * dcPlayerAnimation, dcPlayerAnimationRemove,
 * dcSync, dcSyncDelete, dcEntityFunction
 */
public final class DragonCoreFunctions {

    private DragonCoreFunctions() {}

    public static void register() {
        // dcParticle(scheme) 或 dcParticle(scheme, x, y, z, tile) 或 dcParticle(scheme, x, y, z, tile, targets)
        GlobalFunctions.register("dcParticle", args -> {
            String scheme = ScriptArgs.getString(args, 0);
            if (scheme == null) return null;
            if (args.length >= 5) {
                double x = ScriptArgs.getDouble(args, 1);
                double y = ScriptArgs.getDouble(args, 2);
                double z = ScriptArgs.getDouble(args, 3);
                int tile = ScriptArgs.getInt(args, 4);
                ProxyTargetContainer targets = ScriptArgs.getTargets(args, 5, LeastType.SENDER);
                playParticle(scheme, x, y, z, tile, targets);
            } else {
                ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
                playParticle(scheme, 0.0, 0.0, 0.0, 100, targets);
            }
            return null;
        });

        // dcSound(name) 或 dcSound(name, id, type, volume, pitch, loop) 或 dcSound(name, id, type, volume, pitch, loop, targets)
        GlobalFunctions.register("dcSound", args -> {
            String name = ScriptArgs.getString(args, 0);
            if (name == null) return null;
            if (args.length >= 6) {
                String id = ScriptArgs.getString(args, 1);
                if (id == null) id = UUID.randomUUID().toString();
                String type = ScriptArgs.getString(args, 2);
                if (type == null) type = "music";
                float volume = ScriptArgs.getFloat(args, 3);
                float pitch = ScriptArgs.getFloat(args, 4);
                boolean loop = ScriptArgs.getBoolean(args, 5);
                ProxyTargetContainer targets = ScriptArgs.getTargets(args, 6, LeastType.SENDER);
                playSound(name, id, type, volume, pitch, loop, targets);
            } else {
                ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
                playSound(name, UUID.randomUUID().toString(), "music", 1f, 1f, false, targets);
            }
            return null;
        });

        // dcAnimation(name) 或 dcAnimation(name, transition) 或 dcAnimation(name, transition, targets)
        GlobalFunctions.register("dcAnimation", args -> {
            String name = ScriptArgs.getString(args, 0);
            if (name == null) return null;
            int transition = ScriptArgs.getInt(args, 1);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 2, LeastType.SENDER);
            setAnimation(name, transition, targets);
            return null;
        });

        // dcAnimationRemove(name) 或 dcAnimationRemove(name, transition) 或 dcAnimationRemove(name, transition, targets)
        GlobalFunctions.register("dcAnimationRemove", args -> {
            String name = ScriptArgs.getString(args, 0);
            if (name == null) return null;
            int transition = ScriptArgs.getInt(args, 1);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 2, LeastType.SENDER);
            removeAnimation(name, transition, targets);
            return null;
        });

        // dcPlayerAnimation(name) 或 dcPlayerAnimation(name, targets)
        GlobalFunctions.register("dcPlayerAnimation", args -> {
            String name = ScriptArgs.getString(args, 0);
            if (name == null) return null;
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            for (ProxyTarget<?> target : targets) {
                if (target instanceof ProxyTarget.BukkitEntity) {
                    Object instance = ((ProxyTarget.BukkitEntity) target).getInstance();
                    if (instance instanceof Player) {
                        PacketSender.setPlayerAnimation((Player) instance, name);
                    }
                }
            }
            return null;
        });

        // dcPlayerAnimationRemove() 或 dcPlayerAnimationRemove(targets)
        GlobalFunctions.register("dcPlayerAnimationRemove", args -> {
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 0, LeastType.SENDER);
            for (ProxyTarget<?> target : targets) {
                if (target instanceof ProxyTarget.BukkitEntity) {
                    Object instance = ((ProxyTarget.BukkitEntity) target).getInstance();
                    if (instance instanceof Player) {
                        PacketSender.removePlayerAnimation((Player) instance);
                    }
                }
            }
            return null;
        });

        // dcSync(data) 或 dcSync(data, targets)
        GlobalFunctions.register("dcSync", args -> {
            String data = ScriptArgs.getString(args, 0);
            if (data == null) return null;
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            syncPlaceholder(data, targets);
            return null;
        });

        // dcSyncDelete(name) 或 dcSyncDelete(name, isStartWith) 或 dcSyncDelete(name, isStartWith, targets)
        GlobalFunctions.register("dcSyncDelete", args -> {
            String name = ScriptArgs.getString(args, 0);
            if (name == null) return null;
            boolean isStartWith = ScriptArgs.getBoolean(args, 1);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 2, LeastType.SENDER);
            deletePlaceholderCache(name, isStartWith, targets);
            return null;
        });

        // dcEntityFunction(function) 或 dcEntityFunction(function, targets)
        GlobalFunctions.register("dcEntityFunction", args -> {
            String function = ScriptArgs.getString(args, 0);
            if (function == null) return null;
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            executeEntityFunction(function, targets);
            return null;
        });
    }

    private static void playParticle(String scheme, double x, double y, double z, int tile, ProxyTargetContainer targets) {
        for (ProxyTarget<?> target : targets) {
            String id = UUID.randomUUID().toString();
            String posOrEntityId;
            if (target instanceof ProxyTarget.BukkitEntity) {
                posOrEntityId = ((ProxyTarget.BukkitEntity) target).getInstance().getUniqueId().toString();
            } else if (target instanceof ProxyTarget.Location) {
                ProxyTarget.Location<?> loc = (ProxyTarget.Location<?>) target;
                posOrEntityId = loc.getWorld() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ();
            } else {
                continue;
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                PacketSender.addParticle(player, scheme, id, posOrEntityId, x + "," + y + "," + z, tile);
            }
        }
    }

    private static void playSound(String name, String id, String type, float volume, float pitch, boolean loop, ProxyTargetContainer targets) {
        for (ProxyTarget<?> target : targets) {
            if (target instanceof ProxyTarget.BukkitEntity) {
                Object instance = ((ProxyTarget.BukkitEntity) target).getInstance();
                if (instance instanceof Player) {
                    PacketSender.sendPlaySound((Player) instance, name, id, type, volume, pitch, loop, 0f, 0f, 0f);
                }
            }
        }
    }

    private static void setAnimation(String name, int transition, ProxyTargetContainer targets) {
        for (ProxyTarget<?> target : targets) {
            if (target instanceof ProxyTarget.BukkitEntity) {
                Object instance = ((ProxyTarget.BukkitEntity) target).getInstance();
                if (instance instanceof LivingEntity) {
                    CoreAPI.setEntityAnimation((LivingEntity) instance, name, transition);
                }
            }
        }
    }

    private static void removeAnimation(String name, int transition, ProxyTargetContainer targets) {
        for (ProxyTarget<?> target : targets) {
            if (target instanceof ProxyTarget.BukkitEntity) {
                Object instance = ((ProxyTarget.BukkitEntity) target).getInstance();
                if (instance instanceof LivingEntity) {
                    CoreAPI.removeEntityAnimation((LivingEntity) instance, name, transition);
                }
            }
        }
    }

    private static void syncPlaceholder(String data, ProxyTargetContainer targets) {
        Map<String, String> map = new HashMap<>();
        for (String entry : data.split(" ")) {
            String[] parts = entry.split(",", 2);
            map.put(parts[0], parts.length > 1 ? parts[1] : "");
        }
        for (ProxyTarget<?> target : targets) {
            if (target instanceof ProxyTarget.BukkitEntity) {
                Object instance = ((ProxyTarget.BukkitEntity) target).getInstance();
                if (instance instanceof Player) {
                    PacketSender.sendSyncPlaceholder((Player) instance, map);
                }
            }
        }
    }

    private static void deletePlaceholderCache(String name, boolean isStartWith, ProxyTargetContainer targets) {
        for (ProxyTarget<?> target : targets) {
            if (target instanceof ProxyTarget.BukkitEntity) {
                Object instance = ((ProxyTarget.BukkitEntity) target).getInstance();
                if (instance instanceof Player) {
                    PacketSender.sendDeletePlaceholderCache((Player) instance, name, isStartWith);
                }
            }
        }
    }

    private static void executeEntityFunction(String function, ProxyTargetContainer targets) {
        for (ProxyTarget<?> target : targets) {
            if (target instanceof ProxyTarget.BukkitEntity) {
                Object instance = ((ProxyTarget.BukkitEntity) target).getInstance();
                if (instance instanceof LivingEntity) {
                    UUID entityUuid = ((LivingEntity) instance).getUniqueId();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        PacketSender.runEntityAnimationFunction(player, entityUuid, function);
                    }
                }
            }
        }
    }
}
