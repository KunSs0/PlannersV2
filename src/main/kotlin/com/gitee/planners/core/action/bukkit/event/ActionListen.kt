package com.gitee.planners.core.action.bukkit.event

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.core.action.commandBool
import com.gitee.planners.core.action.commandEnum
import com.gitee.planners.core.action.commandText
import com.gitee.planners.core.action.context.AbstractComplexScriptContext
import com.gitee.planners.core.action.getEnvironmentContext
import com.gitee.planners.module.event.ScriptBlockListener
import com.gitee.planners.module.event.ScriptEventHandler
import taboolib.common.platform.event.EventPriority
import java.util.UUID

object ActionListen {

    @KetherEditor.Document("listen <function> [id: text] on <event> [ignore-cancelled: bool(true)] [priority: string(normal)]")
    @CombinationKetherParser.Used
    fun actionListen() = KetherHelper.combinedKetherParser("listen") {
        it.group(
            symbol(),
            commandText("id", UUID.randomUUID().toString()),
            command("on", then = symbol()),
            commandBool("ignore-cancelled", true),
            commandEnum("priority", EventPriority.NORMAL),
            commandBool("async",false)
        ).apply(it) { blockId,id, eventId, ignoreCancelled, priority,async ->
            now {
                val context = getEnvironmentContext() as AbstractComplexScriptContext
                ScriptEventHandler.registerListener(context.compiled,id,blockId,eventId,ignoreCancelled,priority,async)
            }
        }
    }

    @KetherEditor.Document("unlisten <id: string>")
    @CombinationKetherParser.Used
    fun actionUnlisten() = KetherHelper.combinedKetherParser("unlisten") {
        it.group(text()).apply(it) { id ->
            now {
                val listener = ScriptEventHandler.getListenerById(id) ?: return@now
                ScriptEventHandler.unregisterListener(listener)
            }
        }
    }

}
