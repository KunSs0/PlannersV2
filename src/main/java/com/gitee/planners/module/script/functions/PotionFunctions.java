package com.gitee.planners.module.script.functions;

import com.gitee.planners.api.job.target.LeastType;
import com.gitee.planners.api.job.target.ProxyTarget;
import com.gitee.planners.api.job.target.ProxyTargetContainer;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * 药水效果函数
 * <p>
 * 迁移自 {@code PotionExtensions.kt}
 * <pre>{@code
 * // JS: potion("SPEED", 2, 200)              — 给 sender 加药水
 * // JS: potion("SPEED", 2, 200, targets)      — 给目标加药水
 * // JS: potionRemove("SPEED")                 — 移除 sender 药水
 * // JS: potionRemove("SPEED", targets)         — 移除目标药水
 * }</pre>
 */
public final class PotionFunctions {

    private PotionFunctions() {}

    public static void register() {
        // potion(type, level, duration) 或 potion(type, level, duration, targets)
        GlobalFunctions.register("potion", args -> {
            String typeName = ScriptArgs.getString(args, 0);
            if (typeName == null) return null;
            int level = ScriptArgs.getInt(args, 1);
            int duration = ScriptArgs.getInt(args, 2);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 3, LeastType.SENDER);
            PotionEffectType type = PotionEffectType.getByName(typeName);
            if (type == null) return null;
            PotionEffect effect = new PotionEffect(type, duration, level - 1, false, true);
            for (ProxyTarget<?> t : targets) {
                if (t instanceof ProxyTarget.BukkitEntity) {
                    Object instance = ((ProxyTarget.BukkitEntity) t).getInstance();
                    if (instance instanceof LivingEntity) {
                        ((LivingEntity) instance).addPotionEffect(effect);
                    }
                }
            }
            return null;
        });

        // potionRemove(type) 或 potionRemove(type, targets)
        GlobalFunctions.register("potionRemove", args -> {
            String typeName = ScriptArgs.getString(args, 0);
            if (typeName == null) return null;
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            PotionEffectType type = PotionEffectType.getByName(typeName);
            if (type == null) return null;
            for (ProxyTarget<?> t : targets) {
                if (t instanceof ProxyTarget.BukkitEntity) {
                    Object instance = ((ProxyTarget.BukkitEntity) t).getInstance();
                    if (instance instanceof LivingEntity) {
                        ((LivingEntity) instance).removePotionEffect(type);
                    }
                }
            }
            return null;
        });
    }
}
