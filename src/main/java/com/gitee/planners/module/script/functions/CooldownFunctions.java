package com.gitee.planners.module.script.functions;

import com.gitee.planners.api.job.Skill;
import com.gitee.planners.core.skill.cooler.Cooler;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;

import org.bukkit.entity.Player;

/**
 * Cooldown 冷却系统函数
 * <pre>{@code
 * // JS: getCooldown(skill)              — 获取 sender 的技能剩余冷却
 * // JS: getCooldown(skill, player)      — 获取指定玩家的技能剩余冷却
 * // JS: setCooldown(skill, ticks)       — 设置 sender 的技能冷却
 * // JS: setCooldown(skill, ticks, player) — 设置指定玩家的技能冷却
 * // JS: resetCooldown(skill)            — 重置 sender 的技能冷却
 * // JS: resetCooldown(skill, player)    — 重置指定玩家的技能冷却
 * // JS: hasCooldown(skill)              — 检查 sender 是否在冷却中
 * // JS: hasCooldown(skill, player)      — 检查指定玩家是否在冷却中
 * }</pre>
 */
public final class CooldownFunctions {

    private CooldownFunctions() {}

    public static void register() {
        // getCooldown(skill) 或 getCooldown(skill, player)
        GlobalFunctions.register("getCooldown", args -> {
            Skill skill = ScriptArgs.resolveSkill(ScriptArgs.get(args, 0));
            if (skill == null) return null;
            Player player = ScriptArgs.getPlayer(args, 1);
            if (player == null) return null;
            return Cooler.INSTANCE.get(player, skill);
        });

        // setCooldown(skill, ticks) 或 setCooldown(skill, ticks, player)
        GlobalFunctions.register("setCooldown", args -> {
            Skill skill = ScriptArgs.resolveSkill(ScriptArgs.get(args, 0));
            if (skill == null) return null;
            int ticks = ScriptArgs.getInt(args, 1);
            Player player = ScriptArgs.getPlayer(args, 2);
            if (player == null) return null;
            Cooler.INSTANCE.set(player, skill, ticks);
            return null;
        });

        // resetCooldown(skill) 或 resetCooldown(skill, player)
        GlobalFunctions.register("resetCooldown", args -> {
            Skill skill = ScriptArgs.resolveSkill(ScriptArgs.get(args, 0));
            if (skill == null) return null;
            Player player = ScriptArgs.getPlayer(args, 1);
            if (player == null) return null;
            Cooler.INSTANCE.set(player, skill, 0);
            return null;
        });

        // hasCooldown(skill) 或 hasCooldown(skill, player)
        GlobalFunctions.register("hasCooldown", args -> {
            Skill skill = ScriptArgs.resolveSkill(ScriptArgs.get(args, 0));
            if (skill == null) return null;
            Player player = ScriptArgs.getPlayer(args, 1);
            if (player == null) return null;
            return Cooler.INSTANCE.get(player, skill) > 0;
        });
    }
}
