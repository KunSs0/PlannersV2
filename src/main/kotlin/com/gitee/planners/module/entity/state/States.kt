package com.gitee.planners.module.entity.state

import com.gitee.planners.api.Registries
import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.TargetEntity
import com.gitee.planners.core.config.State
import com.gitee.planners.module.event.ScriptCallback
import com.gitee.planners.module.event.ScriptEventHolder
import com.gitee.planners.module.event.ScriptEventLoader
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.warning
import taboolib.module.kether.ScriptContext

object States {

    /**
     * 初始化所有状态
     */
    @Awake(LifeCycle.ENABLE)
    fun init() {
        for (state in Registries.STATE.values()) {
            load(state)
        }
    }

    /**
     * 插件重载时卸载所有状态
     */
    @SubscribeEvent
    fun e(e: PluginReloadEvents.Pre) {
        for (state in Registries.STATE.values()) {
            unload(state)
        }
    }

    /**
     * 插件重载后重新加载所有状态
     */
    @SubscribeEvent
    fun e(e: PluginReloadEvents.Post) {
        this.init()
    }

    /**
     * 卸载状态
     *
     * @param state 状态
     */
    private fun unload(state: State) {
        for (trigger in state.triggers.values) {
            val holder = ScriptEventLoader.getHolder(trigger.on) ?: continue
            val callback = holder.getCallback(trigger.id)
            if (callback != null) {
                holder.unregister(callback)
            }
        }
    }

    /**
     * 初始化状态
     *
     * @param state 状态
     */
    private fun load(state: State) {
        for (trigger in state.triggers.values) {

            val holder = ScriptEventLoader.getHolder(trigger.on)
            if (holder == null) {
                warning("Unknown script event: ${trigger.on} (state: ${state.id}, trigger: ${trigger.id})")
                continue
            }
            val callbackImpl = ScriptCallbackImpl(state, trigger)

            holder.register(callbackImpl)
        }
    }

    class ScriptCallbackImpl(val state: State, val trigger: State.Trigger) :
        ScriptCallback(trigger.id, trigger.action, true,EventPriority.NORMAL, null, false) {

        override fun <T> call(sender: Target<*>, event: T, holder: ScriptEventHolder<T>) {
            if (sender !is TargetEntity<*>) {
                return
            }

            // 仅生物实体可用
            if (!sender.hasState(state)) {
                return
            }

            super.call(sender, event, holder)
        }

        override fun <T> whenBegin(sender: Target<*>, event: T, holder: ScriptEventHolder<T>, ctx: ScriptContext) {
            super.whenBegin(sender, event, holder, ctx)
            ctx["@State"] = state
            ctx["@Trigger"] = trigger
        }

    }

}