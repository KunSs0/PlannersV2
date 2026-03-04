package com.gitee.planners.module.script.functions;

import com.germ.germplugin.api.GermPacketAPI;
import com.germ.germplugin.api.SoundType;
import com.germ.germplugin.api.ViewType;
import com.germ.germplugin.api.bean.AnimDataDTO;
import com.gitee.planners.api.job.target.LeastType;
import com.gitee.planners.api.job.target.ProxyTarget;
import com.gitee.planners.api.job.target.ProxyTargetContainer;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import taboolib.platform.util.PlatformUtilKt;
import taboolib.type.BukkitEquipment;

import java.util.UUID;

/**
 * GermPlugin 集成函数
 * <p>
 * 迁移自 GermPluginExtensions.kt，合并重载为 13 个全局函数:
 * germEffect, germEffectRemove, germEffectClear,
 * germSound, germAnimation, germAnimationStop,
 * germViewLock, germViewUnlock, germLookLock, germLookUnlock,
 * germMoveLock, germMoveUnlock, germCooldown
 */
public final class GermPluginFunctions {

    private GermPluginFunctions() {}

    public static void register() {
        // germEffect(name) / germEffect(name, index) / germEffect(name, index, targets)
        GlobalFunctions.register("germEffect", args -> {
            String name = ScriptArgs.getString(args, 0);
            if (name == null) return null;
            String index = ScriptArgs.getString(args, 1);
            if (index == null) index = UUID.randomUUID().toString();
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 2, LeastType.SENDER);
            playEffect(name, index, targets);
            return index;
        });

        // germEffectRemove(index) / germEffectRemove(index, targets)
        GlobalFunctions.register("germEffectRemove", args -> {
            String index = ScriptArgs.getString(args, 0);
            if (index == null) return null;
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            forEachPlayer(targets, player -> GermPacketAPI.removeEffect(player, index));
            return null;
        });

        // germEffectClear() / germEffectClear(targets)
        GlobalFunctions.register("germEffectClear", args -> {
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 0, LeastType.SENDER);
            forEachPlayer(targets, GermPacketAPI::clearEffect);
            return null;
        });

        // germSound(name) / germSound(name, type, volume, pitch) / germSound(name, type, volume, pitch, targets)
        GlobalFunctions.register("germSound", args -> {
            String name = ScriptArgs.getString(args, 0);
            if (name == null) return null;
            if (args.length >= 4) {
                String typeName = ScriptArgs.getString(args, 1);
                if (typeName == null) typeName = "MASTER";
                float volume = ScriptArgs.getFloat(args, 2);
                float pitch = ScriptArgs.getFloat(args, 3);
                ProxyTargetContainer targets = ScriptArgs.getTargets(args, 4, LeastType.SENDER);
                SoundType type;
                try {
                    type = SoundType.valueOf(typeName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    type = SoundType.MASTER;
                }
                playGermSound(name, type, volume, pitch, targets);
            } else {
                ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
                playGermSound(name, SoundType.MASTER, 1f, 1f, targets);
            }
            return null;
        });

        // germAnimation(name) / germAnimation(name, speed, reverse) / germAnimation(name, speed, reverse, targets)
        GlobalFunctions.register("germAnimation", args -> {
            String name = ScriptArgs.getString(args, 0);
            if (name == null) return null;
            if (args.length >= 3) {
                float speed = ScriptArgs.getFloat(args, 1);
                boolean reverse = ScriptArgs.getBoolean(args, 2);
                ProxyTargetContainer targets = ScriptArgs.getTargets(args, 3, LeastType.SENDER);
                playAnimation(name, speed, reverse, targets);
            } else {
                ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
                playAnimation(name, 1f, false, targets);
            }
            return null;
        });

        // germAnimationStop(name) / germAnimationStop(name, targets)
        GlobalFunctions.register("germAnimationStop", args -> {
            String name = ScriptArgs.getString(args, 0);
            if (name == null) return null;
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            stopAnimation(name, targets);
            return null;
        });

        // germViewLock() / germViewLock(duration, type) / germViewLock(duration, type, targets)
        GlobalFunctions.register("germViewLock", args -> {
            if (args.length >= 2) {
                long duration = ScriptArgs.getLong(args, 0);
                String typeName = ScriptArgs.getString(args, 1);
                if (typeName == null) typeName = "FIRST_PERSON";
                ProxyTargetContainer targets = ScriptArgs.getTargets(args, 2, LeastType.SENDER);
                ViewType type;
                try {
                    type = ViewType.valueOf(typeName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    type = ViewType.FIRST_PERSON;
                }
                lockView(duration, type, targets);
            } else {
                ProxyTargetContainer targets = ScriptArgs.getTargets(args, 0, LeastType.SENDER);
                lockView(-1L, ViewType.FIRST_PERSON, targets);
            }
            return null;
        });

        // germViewUnlock() / germViewUnlock(targets)
        GlobalFunctions.register("germViewUnlock", args -> {
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 0, LeastType.SENDER);
            forEachPlayer(targets, GermPacketAPI::sendUnlockPlayerCameraView);
            return null;
        });

        // germLookLock() / germLookLock(duration) / germLookLock(duration, targets)
        GlobalFunctions.register("germLookLock", args -> {
            if (args.length >= 1 && args[0] instanceof Number) {
                long duration = ScriptArgs.getLong(args, 0);
                ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
                lockLook(duration, targets);
            } else {
                ProxyTargetContainer targets = ScriptArgs.getTargets(args, 0, LeastType.SENDER);
                lockLook(-1L, targets);
            }
            return null;
        });

        // germLookUnlock() / germLookUnlock(targets)
        GlobalFunctions.register("germLookUnlock", args -> {
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 0, LeastType.SENDER);
            forEachPlayer(targets, GermPacketAPI::sendUnlockPlayerCameraRotate);
            return null;
        });

        // germMoveLock() / germMoveLock(duration) / germMoveLock(duration, targets)
        GlobalFunctions.register("germMoveLock", args -> {
            if (args.length >= 1 && args[0] instanceof Number) {
                long duration = ScriptArgs.getLong(args, 0);
                ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
                lockMove(duration, targets);
            } else {
                ProxyTargetContainer targets = ScriptArgs.getTargets(args, 0, LeastType.SENDER);
                lockMove(-1L, targets);
            }
            return null;
        });

        // germMoveUnlock() / germMoveUnlock(targets)
        GlobalFunctions.register("germMoveUnlock", args -> {
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 0, LeastType.SENDER);
            forEachPlayer(targets, GermPacketAPI::sendUnlockPlayerMove);
            return null;
        });

        // germCooldown(slot, tick) / germCooldown(slot, tick, targets)
        GlobalFunctions.register("germCooldown", args -> {
            String slot = ScriptArgs.getString(args, 0);
            if (slot == null) return null;
            int tick = ScriptArgs.getInt(args, 1);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 2, LeastType.SENDER);
            setCooldown(slot, tick, targets);
            return null;
        });
    }

    private static void playEffect(String name, String index, ProxyTargetContainer targets) {
        for (ProxyTarget<?> target : targets) {
            double x, y, z;
            if (target instanceof ProxyTarget.BukkitEntity) {
                Location loc = ((ProxyTarget.BukkitEntity) target).getBukkitLocation();
                x = loc.getX();
                y = loc.getY();
                z = loc.getZ();
            } else if (target instanceof ProxyTarget.Location) {
                Location loc = ((ProxyTarget.Location<?>) target).getBukkitLocation();
                x = loc.getX();
                y = loc.getY();
                z = loc.getZ();
            } else {
                continue;
            }
            for (Player player : PlatformUtilKt.getOnlinePlayers()) {
                GermPacketAPI.sendEffect(player, name, index, x, y, z);
            }
        }
    }

    private static void playGermSound(String name, SoundType type, float volume, float pitch, ProxyTargetContainer targets) {
        for (ProxyTarget<?> target : targets) {
            if (target instanceof ProxyTarget.BukkitEntity) {
                Object instance = ((ProxyTarget.BukkitEntity) target).getInstance();
                if (instance instanceof Player) {
                    Player player = (Player) instance;
                    Location loc = player.getLocation();
                    GermPacketAPI.playSound(player, name, type, (float) loc.getX(), (float) loc.getY(), (float) loc.getZ(), 0, volume, pitch);
                }
            } else if (target instanceof ProxyTarget.Location) {
                Location loc = ((ProxyTarget.Location<?>) target).getBukkitLocation();
                GermPacketAPI.playSound(loc, name, type, 0, volume, pitch);
            }
        }
    }

    private static void playAnimation(String name, float speed, boolean reverse, ProxyTargetContainer targets) {
        AnimDataDTO animData = new AnimDataDTO(name, speed, reverse);
        for (ProxyTarget<?> target : targets) {
            if (target instanceof ProxyTarget.BukkitEntity) {
                Object instance = ((ProxyTarget.BukkitEntity) target).getInstance();
                int entityId = ((ProxyTarget.BukkitEntity) target).getInstance().getEntityId();
                for (Player sender : PlatformUtilKt.getOnlinePlayers()) {
                    if (instance instanceof Player) {
                        GermPacketAPI.sendBendAction(sender, entityId, animData);
                    } else {
                        GermPacketAPI.sendModelAnimation(sender, entityId, animData);
                    }
                }
            }
        }
    }

    private static void stopAnimation(String name, ProxyTargetContainer targets) {
        for (ProxyTarget<?> target : targets) {
            if (target instanceof ProxyTarget.BukkitEntity) {
                Object instance = ((ProxyTarget.BukkitEntity) target).getInstance();
                int entityId = ((ProxyTarget.BukkitEntity) target).getInstance().getEntityId();
                for (Player sender : PlatformUtilKt.getOnlinePlayers()) {
                    if (instance instanceof Player) {
                        GermPacketAPI.sendBendClear(sender, entityId);
                    } else {
                        GermPacketAPI.stopModelAnimation(sender, entityId, name);
                    }
                }
            }
        }
    }

    private static void lockView(long duration, ViewType type, ProxyTargetContainer targets) {
        forEachPlayer(targets, player -> GermPacketAPI.sendLockPlayerCameraView(player, type, duration));
    }

    private static void lockLook(long duration, ProxyTargetContainer targets) {
        forEachPlayer(targets, player -> GermPacketAPI.sendLockPlayerCameraRotate(player, duration));
    }

    private static void lockMove(long duration, ProxyTargetContainer targets) {
        forEachPlayer(targets, player -> GermPacketAPI.sendLockPlayerMove(player, duration));
    }

    private static void setCooldown(String slot, int tick, ProxyTargetContainer targets) {
        BukkitEquipment equipment;
        try {
            equipment = BukkitEquipment.valueOf(slot.toUpperCase());
        } catch (IllegalArgumentException e) {
            return;
        }
        forEachPlayer(targets, player ->
                GermPacketAPI.setItemStackCooldown(player, equipment.getItem(player), tick)
        );
    }

    @FunctionalInterface
    private interface PlayerAction {
        void accept(Player player);
    }

    private static void forEachPlayer(ProxyTargetContainer targets, PlayerAction action) {
        for (ProxyTarget<?> target : targets) {
            if (target instanceof ProxyTarget.BukkitEntity) {
                Object instance = ((ProxyTarget.BukkitEntity) target).getInstance();
                if (instance instanceof Player) {
                    action.accept((Player) instance);
                }
            }
        }
    }
}
