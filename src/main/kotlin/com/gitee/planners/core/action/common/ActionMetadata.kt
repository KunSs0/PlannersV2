package com.gitee.planners.core.action.common

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.common.script.kether.ParameterKetherParser
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.api.job.target.TargetEntity
import com.gitee.planners.core.action.expectParsedAction
import com.gitee.planners.core.action.expectTargetContainerParsedAction
import com.gitee.planners.core.action.runTargetContainer
import taboolib.module.kether.actionNow
import taboolib.module.kether.run
import taboolib.module.kether.str

@CombinationKetherParser.Used
object ActionMetadata : ParameterKetherParser("metadata") {

    @KetherEditor.Document(value = "metadata <id>", result = Void::class)
    val main = argumentKetherParser("get") { argument ->
        val defaultValue = this.expectParsedAction("def", null)
        val container = this.expectTargetContainerParsedAction(LeastType.SENDER)
        actionNow {
            this.run(argument).str { id ->
                this.run(defaultValue).thenAccept { defaultValue ->
                    this.runTargetContainer(container).thenAccept {
                        it.filterIsInstance<TargetEntity<*>>().first().getMetadata(id)?.any() ?: defaultValue
                    }
                }
            }
        }
    }

}