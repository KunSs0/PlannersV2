package com.gitee.planners.module.script.api;

import com.gitee.planners.api.Registries;
import com.gitee.planners.api.job.target.LeastType;
import com.gitee.planners.api.job.target.ProxyTarget;
import com.gitee.planners.api.job.target.ProxyTargetContainer;
import com.gitee.planners.core.config.State;
import com.gitee.planners.core.skill.entity.state.EntityStateManager;
import com.gitee.planners.module.script.ScriptArgs;
import taboolib.common.platform.function.IOKt;

/**
 * SE 脚本状态 API。
 */
public final class StateAPI {

    public static final StateAPI INSTANCE = new StateAPI();

    private StateAPI() {}

    public void attach(String id, long duration) {
        attach(id, duration, true, null);
    }

    public void attach(String id, long duration, boolean refresh) {
        attach(id, duration, refresh, null);
    }

    public void attach(String id, long duration, boolean refresh, Object targets) {
        if (id == null) {
            return;
        }
        State state = resolveState(id);
        if (state == null) {
            return;
        }
        ProxyTargetContainer container = getTargets(targets, LeastType.SENDER);
        for (ProxyTarget<?> target : container) {
            if (target instanceof ProxyTarget.BukkitEntity) {
                EntityStateManager.INSTANCE.attach((ProxyTarget.Entity<?>) target, state, duration, refresh);
            }
        }
    }

    public void detach(String id) {
        detach(id, 1, null);
    }

    public void detach(String id, int layer) {
        detach(id, layer, null);
    }

    public void detach(String id, int layer, Object targets) {
        if (id == null) {
            return;
        }
        State state = resolveState(id);
        if (state == null) {
            return;
        }
        ProxyTargetContainer container = getTargets(targets, LeastType.SENDER);
        for (ProxyTarget<?> target : container) {
            if (target instanceof ProxyTarget.BukkitEntity) {
                EntityStateManager.INSTANCE.detach((ProxyTarget.Entity<?>) target, state, layer);
            }
        }
    }

    public void remove(String id) {
        remove(id, null);
    }

    public void remove(String id, Object targets) {
        if (id == null) {
            return;
        }
        State state = resolveState(id);
        if (state == null) {
            return;
        }
        ProxyTargetContainer container = getTargets(targets, LeastType.SENDER);
        for (ProxyTarget<?> target : container) {
            if (target instanceof ProxyTarget.BukkitEntity) {
                EntityStateManager.INSTANCE.remove((ProxyTarget.Entity<?>) target, state);
            }
        }
    }

    public boolean has(String id) {
        return has(id, null);
    }

    public boolean has(String id, Object targets) {
        if (id == null) {
            return false;
        }
        State state = resolveState(id);
        if (state == null) {
            return false;
        }
        ProxyTargetContainer container = getTargets(targets, LeastType.SENDER);
        for (ProxyTarget<?> target : container) {
            if (target instanceof ProxyTarget.BukkitEntity) {
                return EntityStateManager.INSTANCE.has((ProxyTarget.Entity<?>) target, state);
            }
        }
        return false;
    }

    private ProxyTargetContainer getTargets(Object targets, LeastType leastType) {
        return ScriptArgs.getTargets(new Object[] { targets }, 0, leastType);
    }

    private State resolveState(String id) {
        State state = Registries.INSTANCE.getSTATE().getOrNull(id);
        if (state == null) {
            IOKt.warning("State '" + id + "' not found!");
        }
        return state;
    }
}
