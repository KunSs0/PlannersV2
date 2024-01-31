package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.job.selector.SelectorRegistry
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.TargetContainer
import taboolib.library.kether.QuestAction
import taboolib.library.kether.QuestReader
import taboolib.module.kether.ScriptFrame
import taboolib.module.kether.expects
import java.util.concurrent.CompletableFuture

class ActionTargetContainer(private val actions: List<QuestAction<out Any>>? = emptyList(), val type: LeastType) : QuestAction<TargetContainer>() {

    override fun process(frame: ScriptFrame): CompletableFuture<TargetContainer> {
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

        val DEFAULT_PREFIX = arrayOf("at","to")

        fun parser(expects: Array<String> = DEFAULT_PREFIX,reader: QuestReader, type: LeastType, ignorePrefix: Boolean = false): ActionTargetContainer {
            val actions = try {
                reader.mark()
                // 如果忽略前缀
                if (!ignorePrefix) {
                    reader.expects(*expects)
                }
                val selectors = mutableListOf<QuestAction<Any>>()
                while (true) {
                    // 尝试捕获一个解析器 如果捕获不到 直接退出本次捕获
                    val expect = SelectorRegistry.getKeys().firstOrNull {
                        try {
                            reader.mark()
                            reader.expect("@$it")
                            true
                        } catch (e: Exception) {
                            reader.reset()
                            false
                        }
                    } ?: break
                    selectors.add(SelectorRegistry.get(expect).action().run().resolve(reader))
                }
                // 如果 actions 为空 则尝试捕获一个变量
                selectors.ifEmpty { listOf(InVariable(reader.nextParsedAction())) }
            } catch (e: Exception) {
                reader.reset()
                null
            }

            return ActionTargetContainer(actions, type)
        }

    }
}
