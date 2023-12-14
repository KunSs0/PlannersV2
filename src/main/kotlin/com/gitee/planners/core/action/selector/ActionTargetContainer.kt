package com.gitee.planners.core.action.selector

import com.gitee.planners.api.job.selector.SelectorRegistry
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.TargetContainer
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.core.action.getEnvironmentContext
import org.bukkit.Bukkit
import taboolib.library.kether.QuestAction
import taboolib.library.kether.QuestReader
import taboolib.module.kether.ScriptFrame
import taboolib.module.kether.expects
import java.util.concurrent.CompletableFuture

class ActionTargetContainer(private val actions: List<QuestAction<out Any>>? = emptyList(),val type: LeastType) : QuestAction<TargetContainer>() {

    override fun process(frame: ScriptFrame) : CompletableFuture<TargetContainer> {
        // 填充容器
        if (actions == null) {
            return CompletableFuture.completedFuture(type.getTargetContainer(frame))
        }

        val container = TargetContainer()
        // temp variable
        frame.variables()["@RUNNING_TEMP_CONTAINER"] = container
        process(frame, 0)
        frame.variables().remove("@RUNNING_TEMP_CONTAINER")
        return CompletableFuture.completedFuture(container)
    }

    private fun process(frame: ScriptFrame, cur: Int): CompletableFuture<Void> {
        return if (cur < actions!!.size) {
            actions[cur].process(frame).thenCompose {
                process(frame, cur + 1)
            }
        } else {
            CompletableFuture.completedFuture(null)
        }
    }

    companion object {


        fun parser(reader: QuestReader,type: LeastType): ActionTargetContainer {
            val actions = try {
                reader.mark()
                reader.expects("at", "to")
                val actions = SelectorRegistry.getKeys().mapNotNull {
                    try {
                        reader.mark()
                        reader.expect("@$it")
                        SelectorRegistry.get(it).action().resolve<Any>(reader)
                    } catch (e: Exception) {
                        reader.reset()
                        null
                    }
                }
                // 如果 actions 为空 则尝试捕获一个变量
                actions.ifEmpty { listOf(InVariable(reader.nextParsedAction())) }
            } catch (e: Exception) {
                reader.reset()
                null
            }
            return ActionTargetContainer(actions,type)
        }

    }

}
