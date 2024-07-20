package com.gitee.planners.module.event

import com.gitee.planners.api.PlayerTemplateAPI.plannersLoaded
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.script.ComplexCompiledScript
import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.kether.context.CompiledScriptContext
import com.gitee.planners.module.kether.context.ImmutableSkillContext
import com.gitee.planners.core.config.ImmutableSkill
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.info
import taboolib.common.platform.function.registerBukkitListener
import taboolib.common.platform.function.unregisterListener
import taboolib.common.platform.function.warning
import taboolib.module.kether.runKether
import java.util.UUID
import java.util.function.Supplier

object ScriptEventHandler {

    val listeners = mutableListOf<ScriptBlockListener>()

    val wrappers = mutableListOf<ScriptEventWrapped<*>>()

    fun <T : Event> registerListener(listener: ScriptBlockListener) {
        val wrapped = getWrapped(listener.event)
        if (wrapped == null) {
            warning("Script Listener ${listener.compiled.id} ${listener.event} is not supported.")
            return
        }
        if (wrapped is ScriptBukkitEventWrapped<*>) {
            wrapped as ScriptBukkitEventWrapped<T>
            listener.mapping =
                registerBukkitListener(wrapped.bind, listener.priority, listener.ignoreCancelled) { event ->
                    val compiled = listener.compiled
                    val sender = wrapped.getSender(event) ?: return@registerBukkitListener
                    // 对技能做出处理
                    val ctx = if (compiled is ImmutableSkill) {
                        // 如果是玩家 则转为技能释放上下文
                        val level =
                            if (sender is TargetBukkitEntity && sender.instance is Player && sender.instance.plannersLoaded) {
                                sender.instance.plannersTemplate.getRegisteredSkillOrNull(compiled.id)?.level ?: 0
                            } else {
                                0
                            }
                        ImmutableSkillContext(sender, compiled, level)
                    } else {
                        CompiledScriptContext(sender, compiled)
                    }
                    runKether {
                        // 关联异步逻辑
                        ctx.async = listener.async
                        // 在event运行环境下，如果非异步行为，强制设置为now
                        if (!ctx.async) {
                            ctx.now = true
                        }
                        ctx.call(listener.block) {
                            context {
                                this.set("id", listener.id)
                                wrapped.handle(event, this)
                            }
                        }
                    }
                }
        }
        listeners += listener
    }

    fun registerListener(
        compiled: ComplexCompiledScript,
        id: String,
        block: String,
        event: String,
        ignoreCancelled: Boolean,
        priority: EventPriority,
        async: Boolean = true
    ): ScriptBlockListener? {
        val optional = compiled.compiledScript().getBlock(block)
        if (optional.isPresent) {
            val listener = ScriptBlockListener(id, compiled, optional.get(), event, priority, ignoreCancelled, async)
            registerListener<Event>(listener)
            return listener
        }
        return null
    }

    fun getListenerById(id: String): ScriptBlockListener? {
        return this.listeners.firstOrNull { it.id == id }
    }

    fun registerListenerForScript(compiled: ComplexCompiledScript) {
        val script = compiled.compiledScript()
        script.getBlock("onload").ifPresent { block ->
            runKether {
                val context = CompiledScriptContext(Bukkit.getConsoleSender().adaptTarget(), compiled)
                compiled.platform().run(UUID.randomUUID().toString(), script, block, context.createOptions())
            }
        }
    }

    @SubscribeEvent
    fun e(e: PluginReloadEvents.Pre) {
        unregisterListeners()
    }

    fun unregisterListeners() {
        listeners.forEach { unregisterListener(it.mapping) }
        listeners.clear()
    }

    fun unregisterListener(listener: ScriptBlockListener) {
        unregisterListener(listener.mapping)
        listeners -= listener
    }

    fun unregisterListener(id: String) {
        this.unregisterListener(getListenerById(id) ?: return)
    }

    fun getListeners(wrapped: ScriptEventWrapped<*>): List<ScriptBlockListener> {
        return listeners.filter { it.event == wrapped.name }
    }

    fun getWrapped(event: String): ScriptEventWrapped<*>? {
        return wrappers.firstOrNull { it.name == event }
    }

    fun registerBukkitWrapper(wrapped: ScriptBukkitEventWrapped<*>) {
        info("register bukkit wrapped ${wrapped.name} ${wrapped.bind}")
        this.wrappers += wrapped
    }

    @Awake
    class Visitor : ClassVisitor(0) {
        override fun getLifeCycle(): LifeCycle {
            return LifeCycle.LOAD
        }

        override fun visitEnd(clazz: Class<*>, instance: Supplier<*>?) {
            if (ScriptBukkitEventWrapped::class.java.isAssignableFrom(clazz)) {
                val wrapped = instance?.get() as? ScriptBukkitEventWrapped<*> ?: return
                registerBukkitWrapper(wrapped)
            }
        }

    }

}
