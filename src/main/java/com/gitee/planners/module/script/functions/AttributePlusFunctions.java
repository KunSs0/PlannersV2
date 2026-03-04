package com.gitee.planners.module.script.functions;

import com.gitee.planners.api.event.player.PlayerDamageEntityEvent;
import com.gitee.planners.api.job.target.LeastType;
import com.gitee.planners.api.job.target.ProxyTarget;
import com.gitee.planners.api.job.target.ProxyTargetContainer;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;
import com.gitee.planners.module.script.ScriptContext;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.serverct.ersha.api.AttributeAPI;
import org.serverct.ersha.attribute.AttributeHandle;
import org.serverct.ersha.attribute.data.AttributeData;
import org.serverct.ersha.attribute.data.AttributeSource;
import taboolib.common.platform.function.AdapterKt;
import taboolib.common.util.SyncKt;
import taboolib.platform.util.ItemUtilKt;

import java.util.Arrays;

/**
 * AttributePlus 属性攻击函数
 * <p>
 * 迁移自 AttributePlusExtensions.kt，合并 4 个重载为 1 个全局函数:
 * <pre>{@code
 * apAttack(attributes)
 * apAttack(attributes, isolation)
 * apAttack(attributes, isolation, targets)
 * apAttack(attributes, isolation, targets, source)
 * }</pre>
 */
public final class AttributePlusFunctions {

    private AttributePlusFunctions() {}

    public static void register() {
        // apAttack(attributes) / apAttack(attributes, isolation) / apAttack(attributes, isolation, targets) / apAttack(attributes, isolation, targets, source)
        GlobalFunctions.register("apAttack", args -> {
            String attributes = ScriptArgs.getString(args, 0);
            if (attributes == null) return null;
            boolean isolation = ScriptArgs.getBoolean(args, 1);
            ProxyTargetContainer targetsArg = ScriptArgs.getTargets(args, 2, LeastType.EMPTY);
            ProxyTargetContainer sourceArg = ScriptArgs.getTargets(args, 3, LeastType.SENDER);
            return executeApAttack(attributes, isolation, targetsArg, sourceArg);
        });
    }

    private static double executeApAttack(
            String attributes,
            boolean isolation,
            ProxyTargetContainer targetsArg,
            ProxyTargetContainer sourceArg
    ) {
        LivingEntity sender = null;
        for (ProxyTarget<?> target : sourceArg) {
            if (target instanceof ProxyTarget.BukkitEntity) {
                Object instance = ((ProxyTarget.BukkitEntity) target).getInstance();
                if (instance instanceof LivingEntity) {
                    sender = (LivingEntity) instance;
                    break;
                }
            }
        }

        if (sender == null) {
            taboolib.common.platform.function.IOKt.warning("apAttack: source not correctly defined");
            return 0.0;
        }

        AttributeData data;
        if (isolation) {
            data = AttributeData.create(sender);
        } else {
            data = AttributeAPI.getAttrData(sender);
        }

        data.operationAttribute(
                AttributeAPI.getAttributeSource(Arrays.asList(attributes.split(","))),
                AttributeSource.OperationType.ADD,
                "planners_skill"
        );

        double totalDamage = 0.0;
        LivingEntity finalSender = sender;

        for (ProxyTarget<?> target : targetsArg) {
            if (!(target instanceof ProxyTarget.BukkitEntity)) continue;
            Object instance = ((ProxyTarget.BukkitEntity) target).getInstance();
            if (!(instance instanceof LivingEntity)) continue;
            LivingEntity entity = (LivingEntity) instance;
            if (entity == finalSender) continue;

            EntityDamageByEntityEvent env = new EntityDamageByEntityEvent(
                    finalSender, entity,
                    EntityDamageEvent.DamageCause.CUSTOM, 0.0
            );

            AttributeHandle handle = SyncKt.runSync(() ->
                    new AttributeHandle(data, AttributeAPI.getAttrData(entity))
            ).init(env, false, true).handleAttackOrDefenseAttribute();

            if (!env.isCancelled() && !handle.isCancelled()) {
                double finalDamage = handle.getDamage(finalSender);

                if (finalDamage > entity.getHealth()) {
                    ItemUtilKt.setMeta(entity, "killer", finalSender);
                }

                handle.sendAttributeMessage();
                SyncKt.runSync(() -> {
                    entity.damage(finalDamage);
                    return null;
                });

                if (finalSender instanceof Player) {
                    new PlayerDamageEntityEvent(
                            (Player) finalSender, entity, finalDamage,
                            EntityDamageEvent.DamageCause.CUSTOM
                    ).call();
                }

                totalDamage += finalDamage;

                double reflectDamage = handle.getDamage(entity);
                if (reflectDamage > 0.0) {
                    AdapterKt.submit(false, false, 0L, 0L, () -> {
                        finalSender.damage(reflectDamage);
                        return null;
                    });
                }
            }
        }

        data.takeApiAttribute("planners_skill");
        return totalDamage;
    }
}
