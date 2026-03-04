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
import com.gitee.planners.module.script.ScriptOptions
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
            val source = state.action ?: continue
            val options = ScriptOptions.of()
            val session = ScriptManager.openSession(options)
            try {
                session.eval(source)
                if (session.hasFunction("main")) {
                    session.invokeFunction("main")
                }
            } catch (e: Exception) {
                warning("Failed to initialize state script: ${state.id}")
                e.printStackTrace()
            } finally {
                session.close()
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
        val source = state.action ?: return
        val target = entity as? ProxyTarget.Entity<*> ?: return
        if (!target.hasState(state)) return

        val options = ScriptOptions.create {
            it.set("sender", target.instance)
            it.set("event", event)
            it.set("state", state)
        }

        val session = ScriptManager.openSession(options)
        try {
            session.eval(source)
            if (session.hasFunction(funcName)) {
                session.invokeFunction(funcName)
            }
        } catch (_: Exception) {
            // 函数不存在或执行失败，忽略
        } finally {
            session.close()
        }
    }
}
