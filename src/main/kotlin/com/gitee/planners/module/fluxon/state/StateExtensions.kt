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

        /**
         * 挂载状态到 sender（默认刷新已有状态时长）
         * @param id 状态 ID 字符串
         * @param duration 持续时间（tick，20 ticks = 1 秒）
         */
        runtime.registerFunction("stateAttach", returns(Type.VOID).params(Type.STRING, Type.NUMBER)) { ctx ->
            val id = ctx.getString(0) ?: return@registerFunction
            val duration = ctx.getAsLong(1)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            attachState(id, duration, true, targets)
        }

        /**
         * 挂载状态到 sender，可选是否刷新已有状态
         * @param id 状态 ID 字符串
         * @param duration 持续时间（tick）
         * @param refresh 是否刷新已有状态时长（true=刷新时长，false=叠加层数）
         */
        runtime.registerFunction("stateAttach", returns(Type.VOID).params(Type.STRING, Type.NUMBER, Type.BOOLEAN)) { ctx ->
            val id = ctx.getString(0) ?: return@registerFunction
            val duration = ctx.getAsLong(1)
            val refresh = ctx.getBool(2)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            attachState(id, duration, refresh, targets)
        }

        /**
         * 挂载状态到指定目标
         * @param id 状态 ID 字符串
         * @param duration 持续时间（tick）
         * @param refresh 是否刷新已有状态时长
         * @param targets 目标实体（支持 Entity/ProxyTarget/容器）
         */
        runtime.registerFunction("stateAttach", returns(Type.VOID).params(Type.STRING, Type.NUMBER, Type.BOOLEAN, Type.OBJECT)) { ctx ->
            val id = ctx.getString(0) ?: return@registerFunction
            val duration = ctx.getAsLong(1)
            val refresh = ctx.getBool(2)
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)
            attachState(id, duration, refresh, targets)
        }

        /**
         * 卸载 sender 的状态（减少 1 层）
         * @param id 状态 ID 字符串
         */
        runtime.registerFunction("stateDetach", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val id = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            detachState(id, 1, targets)
        }

        /**
         * 卸载 sender 的状态指定层数
         * @param id 状态 ID 字符串
         * @param layer 要卸载的层数
         */
        runtime.registerFunction("stateDetach", returns(Type.VOID).params(Type.STRING, Type.NUMBER)) { ctx ->
            val id = ctx.getString(0) ?: return@registerFunction
            val layer = ctx.getAsInt(1)
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            detachState(id, layer, targets)
        }

        /**
         * 卸载目标的状态指定层数
         * @param id 状态 ID 字符串
         * @param layer 要卸载的层数
         * @param targets 目标实体
         */
        runtime.registerFunction("stateDetach", returns(Type.VOID).params(Type.STRING, Type.NUMBER, Type.OBJECT)) { ctx ->
            val id = ctx.getString(0) ?: return@registerFunction
            val layer = ctx.getAsInt(1)
            val targets = ctx.getTargetsArg(2, LeastType.SENDER)
            detachState(id, layer, targets)
        }

        /**
         * 完全移除 sender 的状态（清除所有层数）
         * @param id 状态 ID 字符串
         */
        runtime.registerFunction("stateRemove", returns(Type.VOID).params(Type.STRING)) { ctx ->
            val id = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            removeState(id, targets)
        }

        /**
         * 完全移除目标的状态
         * @param id 状态 ID 字符串
         * @param targets 目标实体
         */
        runtime.registerFunction("stateRemove", returns(Type.VOID).params(Type.STRING, Type.OBJECT)) { ctx ->
            val id = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)
            removeState(id, targets)
        }

        /**
         * 检查 sender 是否拥有指定状态
         * @param id 状态 ID 字符串
         * @return 是否拥有该状态
         */
        runtime.registerFunction("stateHas", returns(Type.BOOLEAN).params(Type.STRING)) { ctx ->
            val id = ctx.getString(0) ?: return@registerFunction
            val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
            ctx.setReturnBool(hasState(id, targets))
        }

        /**
         * 检查目标是否拥有指定状态（检查第一个目标）
         * @param id 状态 ID 字符串
         * @param targets 目标实体
         * @return 是否拥有该状态
         */
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
