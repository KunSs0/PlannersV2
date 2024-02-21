package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.job.selector.Selector
import com.gitee.planners.api.job.selector.SelectorRegistry
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.TargetContainer
import taboolib.library.kether.QuestAction
import taboolib.library.kether.QuestReader
import taboolib.module.kether.ScriptFrame
import taboolib.module.kether.expects
import java.util.concurrent.CompletableFuture

class ActionTargetContainer(private val actions: List<QuestAction<out Any>>? = emptyList(), val type: LeastType) :
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
        ): ActionTargetContainer {
            val actions = try {
                reader.mark()
                // 如果忽略前缀
                if (expects.isNotEmpty()) {
                    reader.expects(*expects)
                }
                val selectors = mutableListOf<QuestAction<Any>>()
                while (true) {

                    var expect : String? = null
                    var filterable = false
                    // 尝试捕获一个解析器 如果捕获不到 直接退出本次捕获
                    processInspect@for (key in SelectorRegistry.getKeys()) {

                        reader.mark()
                        val token = reader.nextToken()
                        if (token == "@$key") {
                            expect = key
                            break@processInspect
                        }
                        // filterable
                        else if (token == "@!$key") {
                            expect = key
                            filterable = SelectorRegistry.get(key) is Selector.Filterable
                            break@processInspect
                        }
                        // 复位 继续下一次检查
                        reader.reset()
                    }
                    // 直接跳出循环
                    if (expect == null) {
                        break
                    }
                    val instance = SelectorRegistry.get(expect)
                    val action = if (filterable) (instance as Selector.Filterable).filter() else instance.select()

                    selectors.add(action.run().resolve(reader))
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

    private class ExpectResult(val id: String, val filterable: Boolean)

}
