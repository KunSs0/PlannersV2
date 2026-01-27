package com.gitee.planners.core.skill.entity.state

import com.gitee.planners.api.Registries
import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.api.event.script.ScriptCustomTriggerEvent
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.hasState
import com.gitee.planners.api.job.target.isExpired
import com.gitee.planners.api.job.target.removeState
import com.gitee.planners.core.config.State
import com.gitee.planners.core.skill.script.ScriptBukkitEventHolder
import com.gitee.planners.core.skill.script.ScriptCallback
import com.gitee.planners.core.skill.script.ScriptEventLoader
import org.bukkit.event.Event
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Schedule
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.warning

object States {

    /**
     * 正在处理的携带状态实体, 实体的状态发生改变并不会从缓存中移除, 需要在适当的时候手动移除
     */
    private val registryCarryStateTarget = mutableListOf<ProxyTarget.Entity<*>>()

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
        init()
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
                // 如果实体携带该状态且状态已过期, 则移除该状态
                if (target.hasState(state) && target.isExpired(state)) {
                    target.removeState(state)
                }
            }
        }
    }

    /**
     * 记录携带状态的实体
     *
     * @param target 实体
     */
    fun record(target: ProxyTarget.Entity<*>) {
        if (target !in registryCarryStateTarget) {
            registryCarryStateTarget.add(target)
        }


    }

    /**
     * 卸载状态
     *
     * @param state 状态
     */
    private fun unload(state: State) {
        for (trigger in state.triggers.values) {
            val holder = ScriptEventLoader.getHolder(trigger.listen) as? ScriptBukkitEventHolder<Event>
            if (holder == null) {
                continue
            }

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

            val holder = ScriptEventLoader.getHolder(trigger.listen)
            if (holder == null) {
                warning("Unknown script event: ${trigger.listen} (state: ${state.id}, trigger: ${trigger.listen})")
                continue
            }

            holder.register(state, trigger)
        }
    }

    /**
     * 触发自定义状态
     *
     * @param sender 发送者
     * @param name 状态名称
     */
    fun trigger(sender: ProxyTarget<*>, name: String) {
        if (sender !is ProxyTarget.Entity<*>) {
            return
        }
        ScriptCustomTriggerEvent(sender, name).call()
    }

}
