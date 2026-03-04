package com.gitee.planners.module.script.functions;

import com.gitee.planners.api.Registries;
import com.gitee.planners.api.job.target.LeastType;
import com.gitee.planners.api.job.target.ProxyTarget;
import com.gitee.planners.api.job.target.ProxyTargetContainer;
import com.gitee.planners.core.config.State;
import com.gitee.planners.core.skill.entity.state.EntityStateManager;
import com.gitee.planners.module.script.GlobalFunctions;
import com.gitee.planners.module.script.ScriptArgs;
import taboolib.common.platform.function.IOKt;

/**
 * 状态机操作全局函数
 * <p>
 * 迁移自 {@code StateExtensions.kt}，合并同名重载。
 * <pre>{@code
 * // JS: stateAttach("燃烧", 40)  stateAttach("燃烧", 40, false)  stateAttach("燃烧", 40, false, targets)
 * // JS: stateDetach("燃烧")  stateDetach("燃烧", 3)  stateDetach("燃烧", 3, targets)
 * // JS: stateRemove("燃烧")  stateRemove("燃烧", targets)
 * // JS: stateHas("燃烧")  stateHas("燃烧", targets)
 * }</pre>
 */
public final class StateFunctions {

    private StateFunctions() {}

    public static void register() {
        // stateAttach(id, duration) / stateAttach(id, duration, refresh) / stateAttach(id, duration, refresh, targets)
        GlobalFunctions.register("stateAttach", args -> {
            String id = ScriptArgs.getString(args, 0);
            if (id == null) return null;
            long duration = ScriptArgs.getLong(args, 1);
            boolean refresh = args.length >= 3 ? ScriptArgs.getBoolean(args, 2) : true;
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 3, LeastType.SENDER);
            State state = resolveState(id);
            if (state == null) return null;
            for (ProxyTarget<?> t : targets) {
                if (t instanceof ProxyTarget.BukkitEntity) {
                    EntityStateManager.INSTANCE.attach((ProxyTarget.Entity<?>) t, state, duration, refresh);
                }
            }
            return null;
        });

        // stateDetach(id) / stateDetach(id, layer) / stateDetach(id, layer, targets)
        GlobalFunctions.register("stateDetach", args -> {
            String id = ScriptArgs.getString(args, 0);
            if (id == null) return null;
            int layer = args.length >= 2 ? ScriptArgs.getInt(args, 1) : 1;
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 2, LeastType.SENDER);
            State state = resolveState(id);
            if (state == null) return null;
            for (ProxyTarget<?> t : targets) {
                if (t instanceof ProxyTarget.BukkitEntity) {
                    EntityStateManager.INSTANCE.detach((ProxyTarget.Entity<?>) t, state, layer);
                }
            }
            return null;
        });

        // stateRemove(id) / stateRemove(id, targets)
        GlobalFunctions.register("stateRemove", args -> {
            String id = ScriptArgs.getString(args, 0);
            if (id == null) return null;
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            State state = resolveState(id);
            if (state == null) return null;
            for (ProxyTarget<?> t : targets) {
                if (t instanceof ProxyTarget.BukkitEntity) {
                    EntityStateManager.INSTANCE.remove((ProxyTarget.Entity<?>) t, state);
                }
            }
            return null;
        });

        // stateHas(id) / stateHas(id, targets)
        GlobalFunctions.register("stateHas", args -> {
            String id = ScriptArgs.getString(args, 0);
            if (id == null) return false;
            ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
            State state = resolveState(id);
            if (state == null) return false;
            for (ProxyTarget<?> t : targets) {
                if (t instanceof ProxyTarget.BukkitEntity) {
                    return EntityStateManager.INSTANCE.has((ProxyTarget.Entity<?>) t, state);
                }
            }
            return false;
        });
    }

    private static State resolveState(String id) {
        State state = Registries.INSTANCE.getSTATE().getOrNull(id);
        if (state == null) {
            IOKt.warning("State '" + id + "' not found!");
        }
        return state;
    }
}
