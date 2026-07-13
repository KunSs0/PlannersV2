package com.gitee.planners.module.script.api;

import com.gitee.planners.api.Registries;
import com.gitee.planners.api.job.target.ProxyTarget;
import com.gitee.planners.core.config.State;
import com.gitee.planners.core.skill.entity.state.EntityStateManager;
import taboolib.common.platform.function.IOKt;

/**
 * SE 脚本状态 API，每次调用仅处理一个明确的实体目标。
 */
public final class StateAPI {

    public static final StateAPI INSTANCE = new StateAPI();

    private StateAPI() {}

    public boolean attach(ProxyTarget.Entity<?> target, String id, long duration) {
        return attach(target, id, duration, true);
    }

    public boolean attach(ProxyTarget.Entity<?> target, String id, long duration, boolean refresh) {
        if (target == null || id == null) {
            return false;
        }
        State state = resolveState(id);
        if (state == null) {
            return false;
        }
        return EntityStateManager.INSTANCE.attach(target, state, duration, refresh);
    }

    public boolean detach(ProxyTarget.Entity<?> target, String id) {
        return detach(target, id, 1);
    }

    public boolean detach(ProxyTarget.Entity<?> target, String id, int layer) {
        if (target == null || id == null) {
            return false;
        }
        State state = resolveState(id);
        if (state == null) {
            return false;
        }
        return EntityStateManager.INSTANCE.detach(target, state, layer);
    }

    public void remove(ProxyTarget.Entity<?> target, String id) {
        if (target == null || id == null) {
            return;
        }
        State state = resolveState(id);
        if (state == null) {
            return;
        }
        EntityStateManager.INSTANCE.remove(target, state);
    }

    public boolean has(ProxyTarget.Entity<?> target, String id) {
        if (target == null || id == null) {
            return false;
        }
        State state = resolveState(id);
        if (state == null) {
            return false;
        }
        return EntityStateManager.INSTANCE.has(target, state);
    }

    public int getLayer(ProxyTarget.Entity<?> target, String id) {
        if (target == null || id == null) {
            return 0;
        }
        State state = resolveState(id);
        if (state == null) {
            return 0;
        }
        return EntityStateManager.INSTANCE.getLayer(target, state);
    }

    private State resolveState(String id) {
        State state = Registries.INSTANCE.getSTATE().getOrNull(id);
        if (state == null) {
            IOKt.warning("State '" + id + "' not found!");
        }
        return state;
    }
}
