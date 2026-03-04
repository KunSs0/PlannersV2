package com.gitee.planners.module.script.functions;

import com.gitee.planners.api.job.target.LeastType;
import com.gitee.planners.api.job.target.ProxyTarget;
import com.gitee.planners.api.job.target.ProxyTargetContainer;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;

import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

/**
 * 实体速度控制函数
 * <p>
 * 迁移自 {@code VelocityExtensions.kt}
 * <pre>{@code
 * // JS: velocitySet(1, 0, 0)            — 设置 sender 速度
 * // JS: velocitySet(1, 0, 0, targets)   — 设置目标速度
 * // JS: velocityAdd(0, 1, 0)            — 叠加速度
 * // JS: velocityMove(0, 0, 1)           — 相对朝向移动
 * // JS: velocityZero()                  — 清零速度
 * // JS: getVelocity(entity)             — 获取速度向量
 * }</pre>
 */
public final class VelocityFunctions {

    private VelocityFunctions() {}

    public static void register() {
        // velocitySet(x, y, z) 或 velocitySet(x, y, z, targets)
        GlobalFunctions.register("velocitySet", args -> {
            double x = ScriptArgs.getDouble(args, 0);
            double y = ScriptArgs.getDouble(args, 1);
            double z = ScriptArgs.getDouble(args, 2);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 3, LeastType.SENDER);
            for (ProxyTarget<?> t : targets) {
                if (t instanceof ProxyTarget.BukkitEntity) {
                    ((ProxyTarget.BukkitEntity) t).getInstance().setVelocity(new Vector(x, y, z));
                }
            }
            return null;
        });

        // velocityAdd(x, y, z) 或 velocityAdd(x, y, z, targets)
        GlobalFunctions.register("velocityAdd", args -> {
            double x = ScriptArgs.getDouble(args, 0);
            double y = ScriptArgs.getDouble(args, 1);
            double z = ScriptArgs.getDouble(args, 2);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 3, LeastType.SENDER);
            for (ProxyTarget<?> t : targets) {
                if (t instanceof ProxyTarget.BukkitEntity) {
                    Entity entity = ((ProxyTarget.BukkitEntity) t).getInstance();
                    entity.setVelocity(entity.getVelocity().add(new Vector(x, y, z)));
                }
            }
            return null;
        });

        // velocityMove(x, y, z) 或 velocityMove(x, y, z, targets)
        GlobalFunctions.register("velocityMove", args -> {
            double x = ScriptArgs.getDouble(args, 0);
            double y = ScriptArgs.getDouble(args, 1);
            double z = ScriptArgs.getDouble(args, 2);
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 3, LeastType.SENDER);
            for (ProxyTarget<?> t : targets) {
                if (t instanceof ProxyTarget.BukkitEntity) {
                    Entity entity = ((ProxyTarget.BukkitEntity) t).getInstance();
                    Vector direction = entity.getLocation().getDirection();
                    Vector velocity = direction.multiply(z)
                            .add(new Vector(0.0, y, 0.0))
                            .add(direction.clone().rotateAroundY(Math.PI / 2).multiply(x));
                    entity.setVelocity(entity.getVelocity().add(velocity));
                }
            }
            return null;
        });

        // velocityZero() 或 velocityZero(targets)
        GlobalFunctions.register("velocityZero", args -> {
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 0, LeastType.SENDER);
            for (ProxyTarget<?> t : targets) {
                if (t instanceof ProxyTarget.BukkitEntity) {
                    ((ProxyTarget.BukkitEntity) t).getInstance().setVelocity(new Vector(0, 0, 0));
                }
            }
            return null;
        });

        // getVelocity(entity)
        GlobalFunctions.register("getVelocity", args -> {
            Object arg = ScriptArgs.get(args, 0);
            if (arg instanceof Entity) {
                return ((Entity) arg).getVelocity();
            }
            return null;
        });
    }
}
