package com.gitee.planners.core.skill.script

import com.gitee.planners.core.config.State
import com.gitee.planners.core.skill.entity.state.ScriptCallbackImpl
import com.gitee.planners.core.skill.script.animated.AbstractEventModifier
import org.bukkit.event.Event
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.ProxyListener
import taboolib.common.platform.function.info
import taboolib.common.platform.function.registerBukkitListener
import taboolib.module.kether.ScriptContext
import taboolib.module.kether.ScriptFrame
import java.util.*

abstract class ScriptBukkitEventHolder<T : Event> : ScriptEventHolder<T> {

    private val callbacks = mutableListOf<ScriptCallback<T>>()

    private val listeners = mutableMapOf<EventPriority,ProxyListener>()

    override fun init() {

    }

    override fun unload() {
        callbacks.clear()
    }

    private fun init(priority: EventPriority) {
        if (this.listeners.containsKey(priority)) {
            return
        }

        listeners[priority] = registerBukkitListener(this.bind, priority, false) {
            for (listener in callbacks.filter { it.priority == priority }) {
                this@ScriptBukkitEventHolder.call(it, listener)
            }
        }
    }

    protected fun call(event: T, callback: ScriptCallback<T>) {
        val sender = this.getSender(event)
        if (sender == null) {
            return
        }
        if (callback.closed) {
            this.unregister(callback)
            return
        }
        callback.call(sender, event, this)
    }

    override fun handle(event: T, ctx: ScriptContext) {
        ctx["event"] = getModifier(event)
    }

    override fun getCallback(id: String): ScriptCallback<T>? {
        return callbacks.firstOrNull { it.id == id }
    }

    /**
     * 分配监听器
     *
     * @param callback 监听器
     */
    open fun getAssignCallback(state: State,trigger: State.Trigger): ScriptCallbackImpl<T> {
        return ScriptCallbackImpl(state,trigger)
    }

    /**
     * 注册监听器
     *
     * @param callback 监听器
     */
    override fun register(state: State, trigger: State.Trigger) {
        val callback = getAssignCallback(state, trigger)
        this.register(callback)
    }

    override fun register(callback: ScriptCallback<T>) {
        if (!this.listeners.containsKey(callback.priority)) {
            this.init(callback.priority)
        }

        callbacks += callback
        info("Registered event listener: ${this.name} (id=${callback.id}, priority=${callback.priority})")
    }

    /**
     * 注销监听器
     *
     * @param callback 监听器
     */
    override fun unregister(callback: ScriptCallback<T>) {
        callbacks -= callback
    }

    companion object {

        fun ScriptFrame.getWrappedEvent(): Optional<AbstractEventModifier<*>> {
            return this.variables().get("event")
        }

    }

}
