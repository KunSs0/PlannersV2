package com.gitee.planners.module.script.functions;

import com.gitee.planners.api.job.target.LeastType;
import com.gitee.planners.api.job.target.ProxyTarget;
import com.gitee.planners.api.job.target.ProxyTargetContainer;
import com.gitee.planners.module.compat.attribute.AttributeDriver;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;
import org.bukkit.entity.LivingEntity;

import java.util.Collections;
import java.util.List;

public final class AttributeDriverFunctions {

    private AttributeDriverFunctions() {}

    public static void register() {
        GlobalFunctions.register("getAttr", args -> {
            String attrName = ScriptArgs.getString(args, 0);
            if (attrName == null) return Collections.emptyList();
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            for (ProxyTarget<?> t : targets) {
                if (!(t instanceof ProxyTarget.BukkitEntity)) continue;
                Object instance = ((ProxyTarget.BukkitEntity) t).getInstance();
                if (!(instance instanceof LivingEntity)) continue;
                return AttributeDriver.Companion.get((LivingEntity) instance, attrName);
            }
            return Collections.emptyList();
        });
    }
}
