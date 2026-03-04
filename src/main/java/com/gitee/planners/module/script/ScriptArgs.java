package com.gitee.planners.module.script;

import com.gitee.planners.api.Registries;
import com.gitee.planners.api.job.Skill;
import com.gitee.planners.api.job.target.LeastType;
import com.gitee.planners.api.job.target.ProxyTarget;
import com.gitee.planners.api.job.target.ProxyTargetContainer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * 脚本参数解析工具类
 * <p>
 * 为 JS 全局函数提供类型安全的参数提取，替代 Fluxon 的 FunctionContext。
 *
 * @see GlobalFunctions
 * @see ScriptContext
 */
public final class ScriptArgs {

    private ScriptArgs() {}

    public static Object get(Object[] args, int index) {
        return index >= 0 && index < args.length ? args[index] : null;
    }

    public static String getString(Object[] args, int index) {
        Object v = get(args, index);
        return v != null ? v.toString() : null;
    }

    public static double getDouble(Object[] args, int index) {
        Object v = get(args, index);
        return v instanceof Number ? ((Number) v).doubleValue() : 0.0;
    }

    public static int getInt(Object[] args, int index) {
        Object v = get(args, index);
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }

    public static long getLong(Object[] args, int index) {
        Object v = get(args, index);
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    public static boolean getBoolean(Object[] args, int index) {
        Object v = get(args, index);
        return v instanceof Boolean && (Boolean) v;
    }

    public static float getFloat(Object[] args, int index) {
        Object v = get(args, index);
        return v instanceof Number ? ((Number) v).floatValue() : 0f;
    }

    /**
     * 解析目标参数，为空时按 leastType 回退到 sender
     */
    public static ProxyTargetContainer getTargets(Object[] args, int index, LeastType leastType) {
        Object arg = get(args, index);
        ProxyTargetContainer targets = resolveTargets(arg);
        if (targets.isEmpty()) {
            Object sender = ScriptContext.getSender();
            return leastType.getTargetContainer(sender);
        }
        return targets;
    }

    /**
     * 解析玩家参数，为空时回退到 sender
     */
    public static Player getPlayer(Object[] args, int index) {
        Object v = get(args, index);
        if (v instanceof Player) return (Player) v;
        if (v == null) {
            Object sender = ScriptContext.getSender();
            return sender instanceof Player ? (Player) sender : null;
        }
        return null;
    }

    /**
     * 解析参数为 LivingEntity
     * <p>
     * 支持: {@link ProxyTarget.BukkitEntity}, {@link LivingEntity}
     */
    public static LivingEntity resolveLivingEntity(Object arg) {
        if (arg instanceof ProxyTarget.BukkitEntity) {
            Entity entity = ((ProxyTarget.BukkitEntity) arg).getInstance();
            return entity instanceof LivingEntity ? (LivingEntity) entity : null;
        }
        if (arg instanceof LivingEntity) {
            return (LivingEntity) arg;
        }
        return null;
    }

    /**
     * 解析任意类型为 ProxyTargetContainer
     * <p>
     * 支持: null, ProxyTargetContainer, ProxyTarget, Entity, Location, Iterable, Array
     */
    public static ProxyTargetContainer resolveTargets(Object arg) {
        if (arg == null) {
            return new ProxyTargetContainer();
        }
        if (arg instanceof ProxyTargetContainer) {
            return (ProxyTargetContainer) arg;
        }
        if (arg instanceof ProxyTarget<?>) {
            return ProxyTargetContainer.Companion.of((ProxyTarget<?>) arg);
        }
        if (arg instanceof Entity) {
            return ProxyTargetContainer.Companion.of(ProxyTarget.Companion.of((Entity) arg));
        }
        if (arg instanceof Location) {
            return ProxyTargetContainer.Companion.of(ProxyTarget.Companion.of((Location) arg));
        }
        if (arg instanceof Iterable<?>) {
            ProxyTargetContainer container = new ProxyTargetContainer();
            for (Object item : (Iterable<?>) arg) {
                container.addAll(resolveTargets(item));
            }
            return container;
        }
        if (arg instanceof Object[]) {
            ProxyTargetContainer container = new ProxyTargetContainer();
            for (Object item : (Object[]) arg) {
                container.addAll(resolveTargets(item));
            }
            return container;
        }
        return new ProxyTargetContainer();
    }

    /**
     * 解析参数为 Skill
     * <p>
     * 支持: String (通过 ID 查找), Skill (直接返回)
     */
    public static Skill resolveSkill(Object arg) {
        if (arg instanceof String) {
            return Registries.INSTANCE.getSKILL().get((String) arg);
        }
        if (arg instanceof Skill) {
            return (Skill) arg;
        }
        return null;
    }
}
