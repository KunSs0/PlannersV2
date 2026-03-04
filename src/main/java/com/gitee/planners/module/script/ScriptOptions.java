package com.gitee.planners.module.script;

import com.gitee.planners.api.PlayerTemplateAPI;
import com.gitee.planners.api.job.target.ProxyTarget;
import com.gitee.planners.core.config.ImmutableSkill;
import com.gitee.planners.core.skill.context.SkillContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 脚本执行选项
 * <p>
 * 包含上下文变量和执行配置 (如 async 标志)。
 */
public class ScriptOptions {

    private final Map<String, Object> variables = new LinkedHashMap<>();
    private boolean async = false;

    public ScriptOptions set(String key, Object value) {
        variables.put(key, value);
        return this;
    }

    public ScriptOptions async(boolean async) {
        this.async = async;
        return this;
    }

    public boolean isAsync() {
        return async;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    // ---- 静态工厂 ----

    public static ScriptOptions of() {
        return new ScriptOptions();
    }

    /**
     * 通用选项 (注入 sender + profile)
     */
    public static ScriptOptions common(Object sender) {
        ScriptOptions options = new ScriptOptions().set("sender", sender);
        if (sender instanceof Player) {
            options.set("profile", ScriptOptions.getProfile((Player) sender));
        }
        return options;
    }

    /**
     * 复制并替换 sender
     */
    public static ScriptOptions sender(Object sender, ScriptOptions base) {
        ScriptOptions options = new ScriptOptions();
        options.variables.putAll(base.variables);
        options.async = base.async;
        options.set("sender", sender);
        return options;
    }

    /**
     * 创建选项 (builder 模式)
     */
    public static ScriptOptions create(Consumer<ScriptOptions> builder) {
        ScriptOptions options = new ScriptOptions();
        builder.accept(options);
        return options;
    }

    /**
     * 创建技能执行选项
     */
    public static ScriptOptions forSkill(Object sender, int level) {
        return forSkill(sender, level, null, null);
    }

    /**
     * 创建技能执行选项
     */
    public static ScriptOptions forSkill(Object sender, int level, Object skill, Map<String, Object> extraVars) {
        ProxyTarget<?> proxyTarget;
        if (sender instanceof ProxyTarget) {
            proxyTarget = (ProxyTarget<?>) sender;
        } else if (sender instanceof Entity) {
            proxyTarget = new ProxyTarget.BukkitEntity((Entity) sender);
        } else {
            proxyTarget = null;
        }

        Player player;
        if (sender instanceof Player) {
            player = (Player) sender;
        } else if (sender instanceof ProxyTarget) {
            Object instance = ((ProxyTarget<?>) sender).getInstance();
            player = instance instanceof Player ? (Player) instance : null;
        } else {
            player = null;
        }

        ImmutableSkill immutableSkill = skill instanceof ImmutableSkill ? (ImmutableSkill) skill : null;

        ScriptOptions options = new ScriptOptions();
        options.set("sender", sender);
        options.set("level", level);
        options.set("ctx", new SkillContext(proxyTarget, immutableSkill, level));
        if (player != null) {
            options.set("profile", getProfile(player));
        }
        if (extraVars != null) {
            extraVars.forEach(options::set);
        }
        return options;
    }

    /**
     * 获取玩家的 Profile (PlayerTemplate)
     */
    static Object getProfile(Player player) {
        return PlayerTemplateAPI.INSTANCE.getPlannersTemplate(player);
    }
}
