package com.gitee.planners.module.kether.bukkit.event

import com.gitee.planners.api.Registries
import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.job.target.TargetContainer
import com.gitee.planners.api.job.target.TargetEntity
import com.gitee.planners.core.config.State
import com.gitee.planners.module.kether.commandLong
import com.gitee.planners.module.kether.commandObjectiveOrSender
import taboolib.common.PrimitiveIO.warning
import taboolib.module.kether.ScriptFrame
import taboolib.module.kether.combinationParser
import java.util.*

@CombinationKetherParser.Used
object ActionState : MultipleKetherParser("state") {

    @KetherEditor.Document("state attach <state: id> [duration <duration:number>] [at objective:TargetContainer(sender)]")
    val attach = combinationParser {
        it.group(
            text(),
            commandLong("duration", -1L),
            commandObjectiveOrSender()
        ).apply(it) { id, duration, objective: TargetContainer ->
            now {
                val state = Registries.STATE.getOrNull(id)
                if (state == null) {
                    warning("State '$id' not found!")
                    return@now
                }

                for (target in objective.filterIsInstance<TargetEntity<*>>()) {
                    target.addState(state, duration)
                }
            }
        }
    }

    @KetherEditor.Document("state detach <state: id> [at objective:TargetContainer(sender)]")
    val detach = combinationParser {
        it.group(text(), commandObjectiveOrSender()).apply(it) { id, objective: TargetContainer ->
            now {
                val state = resolveState(id) ?: return@now

                for (target in objective.filterIsInstance<TargetEntity<*>>()) {
                    target.removeState(state)
                }
            }
        }
    }

    @KetherEditor.Document("state has <state: id> [at objective:TargetContainer(sender)]")
    val has = combinationParser {
        it.group(
            text(),
            commandObjectiveOrSender()
        ).apply(it) { id, objective: TargetContainer ->
            now {
                val state = resolveState(id) ?: return@now false
                val target = objective.filterIsInstance<TargetEntity<*>>().firstOrNull()
                    ?: return@now false
                target.hasState(state)
            }
        }
    }

    @KetherEditor.Document("state contains <state: id> [at objective:TargetContainer(sender)]")
    val contains = has

    /**
     * Resolve state definition either from explicit id or script context placeholder.
     */
    private fun ScriptFrame.resolveState(id: String): State? {
        val resolvedId = if (id == "~") {
            val optional = this.getState()
            if (!optional.isPresent) {
                warning("State context missing for '~' placeholder!")
                return null
            }
            optional.get().id
        } else {
            id
        }

        val state = Registries.STATE.getOrNull(resolvedId)
        if (state == null) {
            warning("State '$resolvedId' not found!")
            return null
        }
        return state
    }

    /**
     * Fetch state object stored in script context.
     */
    private fun ScriptFrame.getState(): Optional<State> {
        return this.variables().get("@State")
    }
}
