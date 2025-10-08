package com.gitee.planners.core.skill.script

import com.gitee.planners.api.common.script.ComplexCompiledScript
import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.kether.context.CompiledScriptContext
import org.bukkit.Bukkit
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
import taboolib.library.reflex.ReflexClass
import taboolib.module.kether.runKether
import java.util.*

object ScriptEventLoader {

    private val holders = mutableMapOf<String, ScriptEventHolder<*>>()

    /**
     * 为脚本注册onload监听器
     *
     * @param compiled 脚本
     */
    fun registerListener(compiled: ComplexCompiledScript) {
        val script = compiled.compiledScript()
        script.getBlock("onload").ifPresent { block ->
            runKether {
                val context = CompiledScriptContext(Bukkit.getConsoleSender().adaptTarget(), compiled)
                compiled.platform().run(UUID.randomUUID().toString(), script, block, context.optionsBuilder())
            }
        }
    }

    @SubscribeEvent
    fun e(e: PluginReloadEvents.Pre) {
        for (holder in holders.values) {
            holder.unload()
        }
    }

    /**
     * 获取事件包装器
     *
     * @param event 事件名
     *
     * @return 事件包装器，若不存在则返回 null
     */
    fun getHolder(event: String): ScriptEventHolder<*>? {
        var holder = holders[event]
        // 解析二级holder
        if (holder == null) {
            val namespace = event.split(" ")[0]
            holder = holders[namespace]
        }

        return holder
    }

    /**
     * 获取回调函数
     *
     * @param id 回调函数 ID
     *
     * @return 回调函数，若不存在则返回 null
     */
    fun getCallback(id: String): ScriptCallback? {
        for (holder in holders.values) {
            val callback = holder.getCallback(id)
            if (callback != null) {
                return callback
            }
        }

        return null
    }

    /**
     * 注册事件包装器
     *
     * @param wrapped 事件包装器
     */
    fun registerHolder(wrapped: ScriptBukkitEventHolder<*>) {
        holders[wrapped.name] = wrapped

        wrapped.init()
    }

    @Awake
    class Visitor : ClassVisitor(0) {
        override fun getLifeCycle(): LifeCycle {
            return LifeCycle.LOAD
        }

        override fun visitEnd(clazz: ReflexClass) {
            if (ScriptBukkitEventHolder::class.java.isAssignableFrom(clazz.toClass())) {
                val wrapped = clazz.getInstance() as? ScriptBukkitEventHolder<*> ?: return
                registerHolder(wrapped)
            }
        }

    }

}
