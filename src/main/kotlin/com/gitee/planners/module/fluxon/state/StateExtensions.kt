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
import com.gitee.planners.module.fluxon.registerFunction
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

        // stateAttach(id, duration, [refresh], [targets]) - 挂载状态
        runtime.registerFunction("stateAttach", listOf(2, 3, 4)) { ctx ->
            val id = ctx.getAsString(0) ?: return@registerFunction null
            val duration = ctx.getAsLong(1)
            val refresh = if (ctx.arguments.size > 2) ctx.getRef(2) as? Boolean ?: true else true
            val targets = ctx.getTargetsArg(3, LeastType.SENDER)

            val state = Registries.STATE.getOrNull(id)
            if (state == null) {
                warning("State '$id' not found!")
                return@registerFunction null
            }

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.attachState(state, duration, refresh)
            }
            null
        }

        // stateDetach(id, [layer], [targets]) - 卸载状态层数
        runtime.registerFunction("stateDetach", listOf(1, 2, 3)) { ctx ->
            val id = ctx.getAsString(0) ?: return@registerFunction null
            val layer = if (ctx.arguments.size > 1) ctx.getAsInt(1) else 1
            val targets = ctx.getTargetsArg(2, LeastType.SENDER)

            val state = Registries.STATE.getOrNull(id)
            if (state == null) {
                warning("State '$id' not found!")
                return@registerFunction null
            }

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.detachState(state, layer)
            }
            null
        }

        // stateRemove(id, [targets]) - 完全移除状态
        runtime.registerFunction("stateRemove", listOf(1, 2)) { ctx ->
            val id = ctx.getAsString(0) ?: return@registerFunction null
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)

            val state = Registries.STATE.getOrNull(id)
            if (state == null) {
                warning("State '$id' not found!")
                return@registerFunction null
            }

            targets.filterIsInstance<ProxyTarget.BukkitEntity>().forEach { target ->
                target.removeState(state)
            }
            null
        }

        // stateHas(id, [targets]) - 检查是否拥有状态
        runtime.registerFunction("stateHas", listOf(1, 2)) { ctx ->
            val id = ctx.getAsString(0) ?: return@registerFunction false
            val targets = ctx.getTargetsArg(1, LeastType.SENDER)

            val state = Registries.STATE.getOrNull(id)
            if (state == null) {
                warning("State '$id' not found!")
                return@registerFunction false
            }

            val target = targets.filterIsInstance<ProxyTarget.BukkitEntity>().firstOrNull()
                ?: return@registerFunction false
            target.hasState(state)
        }
    }
}
