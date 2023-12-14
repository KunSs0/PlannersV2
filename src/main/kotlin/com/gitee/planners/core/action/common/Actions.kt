package com.gitee.planners.core.action.common

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.context.AbstractSkillContext
import com.gitee.planners.api.job.selector.SelectorRegistry
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.TargetCommandSender
import com.gitee.planners.api.job.target.TargetContainer
import com.gitee.planners.core.action.*
import com.gitee.planners.core.action.selector.InVariable
import taboolib.library.kether.QuestAction
import taboolib.module.kether.*
import java.util.concurrent.CompletableFuture


@KetherEditor.Document(value = "lazy <id>", result = Any::class)
@CombinationKetherParser.Used
private fun actionLazyVar() = KetherHelper.combinedKetherParser("lazy") {
    it.group(text()).apply(it) { id ->
        now {
            getEnvironmentContext().castUnsafely<AbstractSkillContext>()?.variables?.get(id)
        }
    }
}

@KetherEditor.Document(value = "tell <message> <at <objective...>>", result = Void::class)
@CombinationKetherParser.Used
private fun actionTell() = KetherHelper.combinedKetherParser("tell") {
    it.group(text(), objective(LeastType.SENDER)).apply(it) { message, container ->
        now {
            container.filterIsInstance<TargetCommandSender<*>>().forEach {
                it.sendMessage(message)
            }
        }
    }
}

