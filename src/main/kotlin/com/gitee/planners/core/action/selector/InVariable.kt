package com.gitee.planners.core.action.selector

import com.gitee.planners.api.job.selector.Selector
import com.gitee.planners.api.job.target.TargetContainer
import com.gitee.planners.core.action.getTargetContainer
import taboolib.library.kether.ParsedAction
import taboolib.library.kether.QuestAction
import taboolib.library.kether.QuestActionParser
import taboolib.library.kether.QuestContext
import taboolib.module.kether.combinationParser
import taboolib.module.kether.run
import taboolib.module.kether.str
import java.util.concurrent.CompletableFuture

// 内部选择器 不开放
class InVariable(val action: ParsedAction<*>) : QuestAction<Void>() {

    override fun process(frame: QuestContext.Frame): CompletableFuture<Void> {
        return frame.run(action).thenAccept { id ->
            val container = if (id is TargetContainer) {
                id
            } else {
                frame.variables().get<TargetContainer>(id.toString()).orElse(TargetContainer())
            }
            frame.getTargetContainer() += container
        }
    }


}

