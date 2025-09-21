package com.gitee.planners.module.kether.bukkit.event

import com.gitee.planners.api.Registries
import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.job.target.TargetContainer
import com.gitee.planners.api.job.target.TargetEntity
import com.gitee.planners.core.config.State
import com.gitee.planners.module.event.ScriptBukkitEventHolder.Companion.getWrappedEvent
import com.gitee.planners.module.event.animated.AbstractEventModifier
import com.gitee.planners.module.kether.commandObjectiveOrSender
import taboolib.common.PrimitiveIO.warning
import taboolib.module.kether.ScriptFrame
import taboolib.module.kether.combinationParser
import java.util.*

@CombinationKetherParser.Used
object ActionState : MultipleKetherParser("state") {

    @KetherEditor.Document("state attach <state: id> [at objective:TargetContainer(sender)]")
    val attach = combinationParser {
        it.group(text(), commandObjectiveOrSender()).apply(it) { id, objective: TargetContainer ->
            now {
                val state = Registries.STATE.getOrNull(id)
                if (state == null) {
                    warning("State '$id' not found!")
                    return@now
                }

                for (target in objective.filterIsInstance<TargetEntity<*>>()) {
                    target.addState(state)
                }
            }
        }
    }

    @KetherEditor.Document("state detach <state: id> [at objective:TargetContainer(sender)]")
    val detach = combinationParser {
        it.group(text(), commandObjectiveOrSender()).apply(it) { id, objective: TargetContainer ->
            now {
                var id = id

                // 根据上下文获取state id
                if (id == "~") {
                    id = this.getState().get().id
                }

                val state = Registries.STATE.getOrNull(id)
                if (state == null) {
                    warning("State '$id' not found!")
                    return@now
                }

                for (target in objective.filterIsInstance<TargetEntity<*>>()) {
                    target.removeState(state)
                }
            }
        }
    }

    /**
     * 获取上下文中的状态
     *
     * @return 状态
     */
    private fun ScriptFrame.getState(): Optional<State> {
        return this.variables().get("@State")
    }

}