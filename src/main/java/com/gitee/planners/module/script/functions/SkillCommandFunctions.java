package com.gitee.planners.module.script.functions;

import com.gitee.planners.api.damage.DamageCause;
import com.gitee.planners.api.damage.ProxyDamage;
import com.gitee.planners.api.job.target.LeastType;
import com.gitee.planners.api.job.target.ProxyTarget;
import com.gitee.planners.api.job.target.ProxyTargetContainer;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;
import org.bukkit.entity.LivingEntity;

/**
 * 技能伤害/治疗全局函数
 * <p>
 * 迁移自 {@code SkillCommands.kt}，合并同名重载。
 * <pre>{@code
 * // JS: damage(50)  damage(50, targets)
 * // JS: damageBy(50, source)  damageBy(50, source, targets)
 * // JS: damageEx(50, "FIRE")  damageEx(50, "FIRE", targets)
 * // JS: damageExBy(50, "FIRE", source)  damageExBy(50, "FIRE", source, targets)
 * // JS: heal(50)  heal(50, targets)
 * }</pre>
 */
public final class SkillCommandFunctions {

    private SkillCommandFunctions() {}

    public static void register() {
        // damage(amount) / damage(amount, targets)
        GlobalFunctions.register("damage", args -> {
            double amount = ScriptArgs.getDouble(args, 0);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            applyDamage(amount, null, DamageCause.Companion.of("SKILL"), targets);
            return null;
        });

        // damageBy(amount, source) / damageBy(amount, source, targets)
        GlobalFunctions.register("damageBy", args -> {
            double amount = ScriptArgs.getDouble(args, 0);
            LivingEntity source = ScriptArgs.resolveLivingEntity(ScriptArgs.get(args, 1));
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 2, LeastType.SENDER);
            applyDamage(amount, source, DamageCause.Companion.of("SKILL"), targets);
            return null;
        });

        // damageEx(amount, cause) / damageEx(amount, cause, targets)
        GlobalFunctions.register("damageEx", args -> {
            double amount = ScriptArgs.getDouble(args, 0);
            String cause = ScriptArgs.getString(args, 1);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 2, LeastType.SENDER);
            applyDamage(amount, null, DamageCause.Companion.of(cause), targets);
            return null;
        });

        // damageExBy(amount, cause, source) / damageExBy(amount, cause, source, targets)
        GlobalFunctions.register("damageExBy", args -> {
            double amount = ScriptArgs.getDouble(args, 0);
            String cause = ScriptArgs.getString(args, 1);
            LivingEntity source = ScriptArgs.resolveLivingEntity(ScriptArgs.get(args, 2));
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 3, LeastType.SENDER);
            applyDamage(amount, source, DamageCause.Companion.of(cause), targets);
            return null;
        });

        // heal(amount) / heal(amount, targets)
        GlobalFunctions.register("heal", args -> {
            double amount = ScriptArgs.getDouble(args, 0);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            applyHeal(amount, targets);
            return null;
        });
    }

    private static void applyDamage(double amount, LivingEntity source, DamageCause cause, ProxyTargetContainer targets) {
        for (ProxyTarget<?> t : targets) {
            if (!(t instanceof ProxyTarget.BukkitEntity)) continue;
            Object instance = ((ProxyTarget.BukkitEntity) t).getInstance();
            if (!(instance instanceof LivingEntity)) continue;
            ProxyDamage.Companion.builder()
                    .source(source)
                    .target((LivingEntity) instance)
                    .damage(amount)
                    .cause(cause)
                    .build()
                    .execute();
        }
    }

    @SuppressWarnings("deprecation")
    private static void applyHeal(double amount, ProxyTargetContainer targets) {
        for (ProxyTarget<?> t : targets) {
            if (!(t instanceof ProxyTarget.BukkitEntity)) continue;
            Object instance = ((ProxyTarget.BukkitEntity) t).getInstance();
            if (!(instance instanceof LivingEntity)) continue;
            LivingEntity entity = (LivingEntity) instance;
            entity.setHealth(Math.min(entity.getHealth() + amount, entity.getMaxHealth()));
        }
    }
}
