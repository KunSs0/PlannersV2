package com.gitee.planners.module.fluxon

import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.core.skill.script.ScriptBukkitEventHolder
import com.gitee.planners.core.skill.script.ScriptCallback
import com.gitee.planners.core.skill.script.ScriptEventHolder
import org.bukkit.event.Event
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.info
import taboolib.library.reflex.ReflexClass
import java.util.concurrent.ConcurrentHashMap

/**
 * Fluxon 事件注册表
 * 统一管理所有事件监听器和触发器
 */
object FluxonEventRegistry {

    private val holders = ConcurrentHashMap<String, ScriptEventHolder<out Any>>()

    /**
     * 注册事件监听器
     * @param name 事件名称
     * @param holder 事件处理器
     */
    fun <T : Event> register(name: String, holder: ScriptEventHolder<T>) {
        holders[name] = holder
        holder.init()
        info("[Fluxon] 注册事件: $name")
    }

    /**
     * 获取事件处理器
     * @param event 事件名称
     * @return 事件处理器，若不存在则返回 null
     */
    fun getHolder(event: String): ScriptEventHolder<*>? {
        var holder = holders[event]
        // 解析命名空间（支持 "namespace subtype" 格式）
        if (holder == null) {
            val namespace = event.split(" ")[0]
            holder = holders[namespace]
        }
        return holder
    }

    /**
     * 获取所有注册的事件名称
     */
    fun getRegisteredEvents(): Set<String> = holders.keys

    /**
     * 获取回调函数
     * @param id 回调函数 ID
     * @return 回调函数，若不存在则返回 null
     */
    fun getCallback(id: String): ScriptCallback<Any>? {
        for (holder in holders.values) {
            val callback = holder.getCallback(id)
            if (callback != null) {
                return callback as ScriptCallback<Any>
            }
        }
        return null
    }

    /**
     * 注销事件处理器
     * @param name 事件名称
     */
    fun unregister(name: String) {
        holders.remove(name)?.unload()
    }

    /**
     * 注销所有事件处理器
     */
    fun unregisterAll() {
        holders.values.forEach { it.unload() }
        holders.clear()
    }

    /**
     * 重载事件处理器
     */
    @SubscribeEvent
    fun onReload(event: PluginReloadEvents.Pre) {
        holders.values.forEach { it.unload() }
    }

    /**
     * 自动扫描并注册事件处理器
     */
    @Awake
    class EventHolderVisitor : ClassVisitor(0) {
        override fun getLifeCycle(): LifeCycle {
            return LifeCycle.LOAD
        }

        override fun visitEnd(clazz: ReflexClass) {
            if (ScriptBukkitEventHolder::class.java.isAssignableFrom(clazz.toClass())) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val holder = clazz.getInstance() as? ScriptBukkitEventHolder<Event> ?: return
                    register(holder.name, holder)
                } catch (e: Exception) {
                    // 忽略无法实例化的类
                }
            }
        }
    }

    /**
     * 初始化内置事件
     */
    fun init() {
        info("[Fluxon] 事件注册表初始化完成，已注册 ${holders.size} 个事件")
    }
}
