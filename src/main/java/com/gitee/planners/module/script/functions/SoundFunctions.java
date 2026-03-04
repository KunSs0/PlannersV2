package com.gitee.planners.module.script.functions;

import com.gitee.planners.api.job.target.LeastType;
import com.gitee.planners.api.job.target.ProxyTarget;
import com.gitee.planners.api.job.target.ProxyTargetContainer;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * 声音播放函数
 * <p>
 * 迁移自 {@code SoundExtensions.kt}
 * <pre>{@code
 * // JS: sound("ENTITY_PLAYER_LEVELUP")                     — 默认音量音调
 * // JS: sound("ENTITY_PLAYER_LEVELUP", 1.5)                — 指定音量
 * // JS: sound("ENTITY_PLAYER_LEVELUP", 1.0, 0.5)           — 指定音量和音调
 * // JS: sound("ENTITY_PLAYER_LEVELUP", 1.0, 0.5, targets)  — 播放给指定目标
 * // JS: soundResource("custom.skill.fire")                  — 资源包声音
 * }</pre>
 */
public final class SoundFunctions {

    private SoundFunctions() {}

    public static void register() {
        // sound(name) / sound(name, volume) / sound(name, volume, pitch) / sound(name, volume, pitch, targets)
        GlobalFunctions.register("sound", args -> {
            String name = ScriptArgs.getString(args, 0);
            if (name == null) return null;
            float volume = args.length > 1 ? ScriptArgs.getFloat(args, 1) : 1.0f;
            float pitch = args.length > 2 ? ScriptArgs.getFloat(args, 2) : 1.0f;
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 3, LeastType.SENDER);
            Sound sound;
            try {
                sound = Sound.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
            for (ProxyTarget<?> t : targets) {
                if (t instanceof ProxyTarget.BukkitEntity) {
                    Object instance = ((ProxyTarget.BukkitEntity) t).getInstance();
                    if (instance instanceof Player) {
                        Player player = (Player) instance;
                        player.playSound(player.getLocation(), sound, volume, pitch);
                    }
                }
            }
            return null;
        });

        // soundResource(name) / soundResource(name, volume) / soundResource(name, volume, pitch) / soundResource(name, volume, pitch, targets)
        GlobalFunctions.register("soundResource", args -> {
            String name = ScriptArgs.getString(args, 0);
            if (name == null) return null;
            float volume = args.length > 1 ? ScriptArgs.getFloat(args, 1) : 1.0f;
            float pitch = args.length > 2 ? ScriptArgs.getFloat(args, 2) : 1.0f;
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 3, LeastType.SENDER);
            for (ProxyTarget<?> t : targets) {
                if (t instanceof ProxyTarget.BukkitEntity) {
                    Object instance = ((ProxyTarget.BukkitEntity) t).getInstance();
                    if (instance instanceof Player) {
                        Player player = (Player) instance;
                        player.playSound(player.getLocation(), name, volume, pitch);
                    }
                }
            }
            return null;
        });
    }
}
