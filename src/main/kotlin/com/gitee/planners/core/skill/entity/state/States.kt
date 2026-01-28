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
import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.event.Event
import org.tabooproject.fluxon.parser.ParsedScript
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Schedule
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.warning

object States {

    /**
     * 内置事件函数名映射
     */
    private val EVENT_FUNC_MAPPING = mapOf(
        "state attach" to "onStateAttach",
        "state detach" to "onStateDetach",
        "state mount" to "onStateMount",
        "state close" to "onStateClose",
        "state end" to "onStateEnd"
    )

    /**
     * 正在处理的携带状态实体, 实体的状态发生改变并不会从缓存中移除, 需要在适当的时候手动移除
     */
    private val registryCarryStateTarget = mutableListOf<ProxyTarget.Entity<*>>()

    /**
     * 每个状态注册的回调列表（用于卸载）
     */
    private val stateCallbacks = mutableMapOf<State, MutableList<Pair<ScriptBukkitEventHolder<*>, ScriptCallback<*>>>>()

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
        // 注销所有回调
        stateCallbacks[state]?.forEach { (holder, callback) ->
            @Suppress("UNCHECKED_CAST")
            (holder as ScriptBukkitEventHolder<Event>).unregister(callback as ScriptCallback<Event>)
        }
        stateCallbacks.remove(state)
    }

    /**
     * 初始化状态
     *
     * @param state 状态
     */
    private fun load(state: State) {
        val script = state.action ?: return

        stateCallbacks[state] = mutableListOf()

        // 创建临时环境执行 main() 函数（如果存在）
        val initEnv = script.newEnvironment()
        try {
            script.eval(initEnv)
            // 尝试调用 main()
            try {
                val mainCall = FluxonScriptCache.getOrParse("main()")
                mainCall.eval(initEnv)
            } catch (_: Exception) {
                // main() 不存在，忽略
            }
        } catch (e: Exception) {
            warning("Failed to initialize state script: ${state.id}")
            e.printStackTrace()
            return
        }

        // 检测并注册内置事件回调
        for ((eventName, funcName) in EVENT_FUNC_MAPPING) {
            // 检测函数是否存在
            if (!hasFunctionDefined(script, funcName)) {
                continue
            }

            val holder = ScriptEventLoader.getHolder(eventName) as? ScriptBukkitEventHolder<Event>
            if (holder == null) {
                warning("Unknown script event: $eventName (state: ${state.id}, func: $funcName)")
                continue
            }

            val callback = ScriptCallbackImpl<Event>(state, funcName, script)
            holder.register(callback)
            stateCallbacks[state]!!.add(holder to callback)
        }
    }

    /**
     * 检测脚本中是否定义了指定函数
     */
    private fun hasFunctionDefined(script: ParsedScript, funcName: String): Boolean {
        return try {
            val env = script.newEnvironment()
            script.eval(env)
            // 尝试解析函数调用，如果函数不存在会抛异常
            val checkScript = FluxonScriptCache.getOrParse("$funcName()")
            checkScript.eval(env)
            true
        } catch (_: Exception) {
            false
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
