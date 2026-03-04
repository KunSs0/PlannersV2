package com.gitee.planners.module.script.functions;

import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * 技能系统函数
 * <p>
 * 合并 apAttack 的两个重载:
 * <pre>{@code
 * // JS: apAttack("damage=50", targets)
 * // JS: apAttack("damage=50", targets, source)
 * }</pre>
 */
public final class SkillSystemFunctions {

    private SkillSystemFunctions() {}

    public static void register() {
        // apAttack(params, targets) 或 apAttack(params, targets, source)
        GlobalFunctions.register("apAttack", args -> {
            String params = ScriptArgs.getString(args, 0);
            if (params == null) return null;
            Object targetsArg = ScriptArgs.get(args, 1);
            if (targetsArg == null) return null;
            Map<String, String> paramMap = parseAttackParams(params);
            double damage = 0.0;
            String dmgStr = paramMap.get("damage");
            if (dmgStr != null) {
                try {
                    damage = Double.parseDouble(dmgStr);
                } catch (NumberFormatException ignored) {}
            }
            if (targetsArg instanceof Iterable<?>) {
                for (Object item : (Iterable<?>) targetsArg) {
                    if (item instanceof LivingEntity) {
                        LivingEntity living = (LivingEntity) item;
                        if (!living.isDead()) {
                            living.damage(damage);
                        }
                    }
                }
            } else if (targetsArg instanceof Object[]) {
                for (Object item : (Object[]) targetsArg) {
                    if (item instanceof LivingEntity) {
                        LivingEntity living = (LivingEntity) item;
                        if (!living.isDead()) {
                            living.damage(damage);
                        }
                    }
                }
            }
            return null;
        });
    }

    private static Map<String, String> parseAttackParams(String params) {
        Map<String, String> map = new HashMap<>();
        for (String pair : params.split(",")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                map.put(parts[0].trim(), parts[1].trim());
            }
        }
        return map;
    }
}
