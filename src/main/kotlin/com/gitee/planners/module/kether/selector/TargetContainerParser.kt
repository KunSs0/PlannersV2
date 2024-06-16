package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.job.selector.Selector
import com.gitee.planners.api.job.selector.SelectorRegistry
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.TargetContainer
import taboolib.common.platform.function.warning
import taboolib.library.kether.QuestAction
import taboolib.library.kether.QuestReader
import taboolib.module.kether.ScriptFrame
import taboolib.module.kether.expects
import java.util.concurrent.CompletableFuture

class TargetContainerParser(private val actions: List<QuestAction<out Any>>? = emptyList(), val type: LeastType) :
    QuestAction<TargetContainer>() {

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
            }.exceptionally {
                warning("Error while processing target container action: ${it.message}")
                null
            }
        } else {
            CompletableFuture.completedFuture(null)
        }
    }

    companion object {

        val DEFAULT_PREFIX = arrayOf("at", "to")

        fun parser(
            expects: Array<String> = DEFAULT_PREFIX,
            reader: QuestReader,
            type: LeastType,
        ): TargetContainerParser {
            val actions = try {
                reader.mark()
                // 如果忽略前缀
                if (expects.isNotEmpty()) {
                    reader.expects(*expects)
                }
                val selectors = mutableListOf<QuestAction<Any>>()
                while (true) {
                    // 标记脚本索引
                    reader.mark()
                    // 拿出一个选择器命令
                    val token = reader.nextToken()
                    // 如果不是选择器 则退出选择器解析
                    if (token[0] != '@') {
                        reader.reset()
                        break
                    }
                    // 是否是过滤选择器
                    val filterable = token[1] == '!'
                    val selector =
                        SelectorRegistry.getOrNull(if (filterable) token.substring(2) else token.substring(1))
                    // 如果没有捕捉到一个合适的选择器 退出选择器解析
                    if (selector == null) {
                        reader.reset()
                        break
                    }
                    val action = if (filterable) (selector as Selector.Filterable).filter() else selector.select()
                    selectors.add(action.run().resolve(reader))
                }
                // 如果 actions 为空 则尝试至少捕获一个变量
                selectors.ifEmpty { listOf(InVariable(reader.nextParsedAction())) }
            } catch (e: Exception) {
                reader.reset()
                null
            }

            return TargetContainerParser(actions, type)
        }

    }

}
