package com.gitee.planners.module.fluxon.state

import com.gitee.planners.api.Registries
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.attachState
import com.gitee.planners.api.job.target.detachState
import com.gitee.planners.api.job.target.hasState
import com.gitee.planners.api.job.target.removeState
import com.gitee.planners.module.fluxon.FluxonScriptCache

import com.gitee.planners.module.fluxon.getTargetsArg
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.warning

/**
 * 状态机操作扩展
 */
object StateExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // stateAttach(id, duration) - 挂载状态到 sender
        runtime.registerFunction("stateAttach", returns(Type.VOID).params(Type.STRING, Type.NUMBER)) { ctx ->
            val id = ctx.getString(0) ?: return@registerFunction
            val duration = ctx.getAsLong(1)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            attachState(id, duration, true, targets)
        }

        // stateAttach(id, duration, refresh) - 挂载状态到 sender，可选刷新
        runtime.registerFunction("stateAttach", returns(Type.VOID).params(Type.STRING, Type.NUMBER, Type.BOOLEAN)) { ctx ->
            val id = ctx.getString(0) ?: return@registerFunction
            val duration = ctx.getAsLong(1)
            val refresh = ctx.getBool(2)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            attachState(id, duration, refresh, targets)
        }

        // stateAttach(id, duration, refresh, targets) - 挂载状态到目标
        runtime.registerFunction("stateAttach", returns(Type.VOID).params(Type.STRING, Type.NUMBER, Type.BOOLEAN, Type.OBJECT)) { ctx ->
            val id = ctx.getString(0) ?: return@registerFunction
            val duration = ctx.getAsLong(1)
            val refresh = ctx.getBool(2)
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)
            attachState(id, duration, refresh, targets)
        }

        // stateDetach(id) - 卸载 sender 的状态层数
        runtime.registerFunction("stateDetach", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val id = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            detachState(id, 1, targets)
        }

        // stateDetach(id, layer) - 卸载 sender 的状态指定层数
        runtime.registerFunction("stateDetach", returns(Type.VOID).params(Type.STRING, Type.NUMBER)) { ctx ->
            val id = ctx.getString(0) ?: return@registerFunction
            val layer = ctx.getAsInt(1)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            detachState(id, layer, targets)
        }

        // stateDetach(id, layer, targets) - 卸载目标的状态层数
        runtime.registerFunction("stateDetach", returns(Type.VOID).params(Type.STRING, Type.NUMBER, Type.OBJECT)) { ctx ->
            val id = ctx.getString(0) ?: return@registerFunction
            val layer = ctx.getAsInt(1)
            val targets = ctx.getTargetsArg(2, LeastType.SENDER)
            detachState(id, layer, targets)
        }

        // stateRemove(id) - 完全移除 sender 的状态
        runtime.registerFunction("stateRemove", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val id = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            removeState(id, targets)
        }

        // stateRemove(id, targets) - 完全移除目标的状态
        runtime.registerFunction("stateRemove", returns(Type.VOID).params(Type.STRING, Type.OBJECT)) { ctx ->
            val id = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            removeState(id, targets)
        }

        // stateHas(id) - 检查 sender 是否拥有状态
        runtime.registerFunction("stateHas", returns(Type.BOOLEAN).params(Type.STRING)) { ctx ->
            val id = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            ctx.setReturnBool(hasState(id, targets))
        }

        // stateHas(id, targets) - 检查目标是否拥有状态
        runtime.registerFunction("stateHas", returns(Type.BOOLEAN).params(Type.STRING, Type.OBJECT)) { ctx ->
            val id = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            ctx.setReturnBool(hasState(id, targets))
        }
    }

    private fun attachState(id: String, duration: Long, refresh: Boolean, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        val state = Registries.STATE.getOrNull(id)
        if (state == null) {
            warning("State '$id' not found!")
            return
        }
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            target.attachState(state, duration, refresh)
        }
    }

    private fun detachState(id: String, layer: Int, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        val state = Registries.STATE.getOrNull(id)
        if (state == null) {
            warning("State '$id' not found!")
            return
        }
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            target.detachState(state, layer)
        }
    }

    private fun removeState(id: String, targets: com.gitee.planners.api.job.target.ProxyTargetContainer) {
        val state = Registries.STATE.getOrNull(id)
        if (state == null) {
            warning("State '$id' not found!")
            return
        }
        targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
            target.removeState(state)
        }
    }

    private fun hasState(id: String, targets: com.gitee.planners.api.job.target.ProxyTargetContainer): Boolean {
        val state = Registries.STATE.getOrNull(id)
        if (state == null) {
            warning("State '$id' not found!")
            return false
        }
        val target = targets.filterIsInstance<ProxyTarget.BukkitEntity>().firstOrNull() ?: return false
        return target.hasState(state)
    }
}
