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
import com.gitee.planners.module.action.ActionRunner
import com.gitee.planners.module.action.ActionScopeFactory
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.Player
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
            runFlow(state, null, null, State.FLOW_LOAD)
        }
    }

    @SubscribeEvent
    fun e(e: PluginReloadEvents.Post) {
        init()
    }

    @SubscribeEvent
    fun onStateAttach(e: EntityStateEvent.Attach.Post) {
        invokeCallback(e.state, e.entity, e, State.FLOW_ATTACH)
    }

    @SubscribeEvent
    fun onStateDetach(e: EntityStateEvent.Detach.Pre) {
        invokeCallback(e.state, e.entity, e, State.FLOW_DETACH)
    }

    @SubscribeEvent
    fun onStateMount(e: EntityStateEvent.Mount.Post) {
        invokeCallback(e.state, e.entity, e, State.FLOW_MOUNT)
    }

    @SubscribeEvent
    fun onStateClose(e: EntityStateEvent.Close.Pre) {
        invokeCallback(e.state, e.entity, e, State.FLOW_CLOSE)
    }

    @SubscribeEvent
    fun onStateEnd(e: EntityStateEvent.End) {
        invokeCallback(e.state, e.entity, e, State.FLOW_END)
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

    private fun invokeCallback(
        state: State,
        entity: ProxyTarget<*>,
        event: Any,
        flowName: String,
    ) {
        val target = entity as? ProxyTarget.Entity<*>
        if (target == null) {
            return
        }
        if (!target.hasState(state)) {
            return
        }
        runFlow(state, target, event, flowName)
    }

    /**
     * 执行状态定义的命名流程。
     *
     * 状态没有技能上下文，因此只绑定实体本身；事件对象放进载荷。
     *
     * @param state 状态定义。
     * @param target 目标；`onLoad` 阶段没有目标，传 null。
     * @param event 触发本次流程的事件对象。
     * @param flowName 流程名。
     */
    private fun runFlow(state: State, target: ProxyTarget.Entity<*>?, event: Any?, flowName: String) {
        val spec = state.flows
        if (spec == null || !spec.hasFlow(flowName)) {
            return
        }
        val payload = LinkedHashMap<String, Any?>()
        if (event != null) {
            payload["event"] = event
        }
        payload["state"] = state
        val instance = target?.instance
        val player = instance as? Player
        val scope = if (player != null) {
            ActionScopeFactory.forPlayer(player, null, 0, emptyMap(), target, "state:${state.id}", payload)
        } else {
            QueryScope(sender = instance, label = "state:${state.id}", payload = payload)
        }
        try {
            ActionRunner.run(spec, flowName, scope)
        } catch (exception: Exception) {
            warning("Failed to run state flow '$flowName' for '${state.id}': ${exception.message}")
        }
    }
}
