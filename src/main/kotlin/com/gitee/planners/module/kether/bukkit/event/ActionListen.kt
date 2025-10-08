package com.gitee.planners.module.kether.bukkit.event

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.core.skill.script.ScriptCallback
import com.gitee.planners.core.skill.script.ScriptEventHolder
import com.gitee.planners.core.skill.script.ScriptEventLoader
import com.gitee.planners.module.kether.commandBool
import com.gitee.planners.module.kether.commandEnum
import com.gitee.planners.module.kether.context.AbstractComplexScriptContext
import com.gitee.planners.module.kether.getEnvironmentContext
import taboolib.common.OpenResult
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.function.warning
import taboolib.common.util.orNull
import taboolib.module.kether.KetherProperty
import taboolib.module.kether.ScriptProperty
import java.util.*

object ActionListen {

    @KetherEditor.Document("listen <function> on <event> [ignore-cancelled: bool(true)] [priority: string(normal)]")
    @CombinationKetherParser.Used
    fun actionListen() = KetherHelper.combinedKetherParser("listen") {
        it.group(
            symbol(),
            command("on", then = symbol()),
            command("ignore-cancelled", "ig", then = bool()).option().defaultsTo(false),
            commandEnum("priority", EventPriority.NORMAL),
            commandBool("async", false)
        ).apply(it) { blockId, eventId, ignoreCancelled, priority, async ->
            now {
                val id = UUID.randomUUID().toString()
                val context = getEnvironmentContext() as AbstractComplexScriptContext
                val holder = ScriptEventLoader.getHolder(eventId) as? ScriptEventHolder<Any>
                if (holder == null) {
                    warning("The event holder of '$eventId' is not found.")
                    return@now
                }
                val block = context.compiled.getBlockScript(blockId).orNull()
                if (block == null) {
                    warning("The block script of '$blockId' is not found.")
                    return@now
                }
                val callback = ScriptCallback<Any>(id, context.compiled, ignoreCancelled, priority, block, async)
                holder.register(callback)
            }
        }
    }

    @KetherEditor.Document("unlisten")
    @CombinationKetherParser.Used
    fun actionUnlisten() = KetherHelper.combinedKetherParser("unlisten") {
        it.group(command("id", then = symbol()).optional()).apply(it) { id ->
            now {
                val id = id.orElseGet { this.variables().get<String>("id").get() }

                val callback = ScriptEventLoader.getCallback(id)
                if (callback != null) {
                    callback.closed = true
                }
            }
        }
    }

    @KetherProperty(bind = ScriptCallback::class)
    fun getWrappedEvent() = object : ScriptProperty<ScriptCallback<Any>>("scriptblocklistener.operator") {

        override fun write(instance: ScriptCallback<Any>, key: String, value: Any?): OpenResult {
            return OpenResult.failed()
        }

        override fun read(instance: ScriptCallback<Any>, key: String): OpenResult {
            return when (key) {

                "id" -> OpenResult.successful(instance.id)

                else -> OpenResult.failed()
            }
        }

    }

}
