package com.gitee.planners.module.kether.bukkit.event

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.module.event.ScriptBlockListener
import com.gitee.planners.module.event.ScriptBukkitEventWrapped.Companion.getWrappedEvent
import com.gitee.planners.module.kether.commandBool
import com.gitee.planners.module.kether.commandEnum
import com.gitee.planners.module.kether.commandText
import com.gitee.planners.module.kether.context.AbstractComplexScriptContext
import com.gitee.planners.module.kether.getEnvironmentContext
import com.gitee.planners.module.event.ScriptEventHandler
import taboolib.common.OpenResult
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.function.warning
import taboolib.module.kether.KetherProperty
import taboolib.module.kether.ScriptProperty
import taboolib.module.kether.actionNow
import java.util.UUID

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
                ScriptEventHandler.registerListener(
                    context.compiled,
                    id,
                    blockId,
                    eventId,
                    ignoreCancelled,
                    priority,
                    async
                )
            }
        }
    }

    @KetherEditor.Document("unlisten")
    @CombinationKetherParser.Used
    fun actionUnlisten() = KetherHelper.combinedKetherParser("unlisten") {
        it.group(
            command("id", then = symbol()).optional()
        ).apply(it) { id ->
            now {
                if (id.isPresent) {
                    ScriptEventHandler.unregisterListener(id.get())
                } else if (this.getWrappedEvent().isPresent) {
                    @Suppress("NAME_SHADOWING")
                    val id = this.variables().get<String>("id")

                    if (id.isPresent) {
                        ScriptEventHandler.unregisterListener(id.get())
                    } else {
                        warning("The id is not found with wrapped event.")
                    }
                }
            }
        }
    }

    @KetherProperty(bind = ScriptBlockListener::class)
    fun getWrappedEvent() = object : ScriptProperty<ScriptBlockListener>("scriptblocklistener.operator") {

        override fun write(instance: ScriptBlockListener, key: String, value: Any?): OpenResult {
            return OpenResult.failed()
        }

        override fun read(instance: ScriptBlockListener, key: String): OpenResult {
            return when (key) {

                "id" -> OpenResult.successful(instance.id)

                else -> OpenResult.failed()
            }
        }

    }

}
