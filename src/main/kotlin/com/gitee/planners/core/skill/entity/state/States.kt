package com.gitee.planners.core.skill.entity.state

import com.gitee.planners.api.Registries
import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.api.event.entity.EntityStateEvent
import com.gitee.planners.api.event.script.ScriptCustomTriggerEvent
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.hasState
import com.gitee.planners.api.job.target.isExpired
import com.gitee.planners.api.job.target.removeState
import com.gitee.planners.core.config.State
import com.gitee.planners.module.script.ScriptManager
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Schedule
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.warning

object States {

    private val registryCarryStateTarget = mutableListOf<ProxyTarget.Entity<*>>()

    @Awake(LifeCycle.ENABLE)
    fun init() {
        for (state in Registries.STATE.values()) {
            val module = state.actionModule
            if (module == null) {
                continue
            }
            try {
                ScriptManager.invokeBusiness(module, "onLoad", emptyMap())
            } catch (e: Exception) {
                warning("Failed to initialize state script: ${state.id}")
                e.printStackTrace()
            }
        }
    }

    @SubscribeEvent
    fun e(e: PluginReloadEvents.Post) {
        init()
    }

    @SubscribeEvent
    fun onStateAttach(e: EntityStateEvent.Attach.Post) {
        invokeCallback(e.state, e.entity, e, "onStateAttach")
    }

    @SubscribeEvent
    fun onStateDetach(e: EntityStateEvent.Detach.Pre) {
        invokeCallback(e.state, e.entity, e, "onStateDetach")
    }

    @SubscribeEvent
    fun onStateMount(e: EntityStateEvent.Mount.Post) {
        invokeCallback(e.state, e.entity, e, "onStateMount")
    }

    @SubscribeEvent
    fun onStateClose(e: EntityStateEvent.Close.Pre) {
        invokeCallback(e.state, e.entity, e, "onStateClose")
    }

    @SubscribeEvent
    fun onStateEnd(e: EntityStateEvent.End) {
        invokeCallback(e.state, e.entity, e, "onStateEnd")
    }

    @Schedule
    fun tick() {
        val iterator = registryCarryStateTarget.iterator()
        while (iterator.hasNext()) {
            val target = iterator.next()
            if (target.isValid()) {
                iterator.remove()
                continue
            }
            for (state in Registries.STATE.values()) {
                if (target.hasState(state) && target.isExpired(state)) {
                    target.removeState(state)
                }
            }
        }
    }

    fun record(target: ProxyTarget.Entity<*>) {
        if (target !in registryCarryStateTarget) {
            registryCarryStateTarget.add(target)
        }
    }

    fun trigger(sender: ProxyTarget<*>, name: String) {
        if (sender !is ProxyTarget.Entity<*>) {
            return
        }
        ScriptCustomTriggerEvent(sender, name).call()
    }

    private fun invokeCallback(state: State, entity: ProxyTarget<*>, event: Any, funcName: String) {
        val module = state.actionModule
        if (module == null) {
            return
        }
        val target = entity as? ProxyTarget.Entity<*>
        if (target == null) {
            return
        }
        if (!target.hasState(state)) {
            return
        }

        val bindings = LinkedHashMap<String, Any?>()
        bindings["sender"] = target.instance
        bindings["event"] = event
        bindings["state"] = state

        try {
            ScriptManager.invokeBusiness(module, funcName, bindings)
        } catch (exception: Exception) {
            warning("Failed to invoke state callback '$funcName' for '${state.id}': ${exception.message}")
        }
    }
}
