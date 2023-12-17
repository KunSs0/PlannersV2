package com.gitee.planners.core.action.common

import com.gitee.planners.api.common.script.KetherEditor
import com.gitee.planners.api.common.script.kether.CombinationKetherParser
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.MultipleKetherParser
import com.gitee.planners.api.job.context.AbstractSkillContext
import com.gitee.planners.api.job.context.ImmutableSkillContext
import com.gitee.planners.api.job.target.LeastType
import com.gitee.planners.core.action.commandObjective
import com.gitee.planners.core.action.getEnvironmentContext

@KetherEditor.Document("ctx ...")
@CombinationKetherParser.Used
object ActionContext : MultipleKetherParser("ctx", "context") {

    @KetherEditor.Document("ctx origin ...")
    val origin = object : MultipleKetherParser() {

        @KetherEditor.Document("ctx origin to [at objective:TargetContainer(sender)]")
        val set = KetherHelper.combinedKetherParser("to") {
            it.group(commandObjective(LeastType.SENDER)).apply(it) { objective ->
                now {
                    getEnvironmentContext().origin = objective.firstOrNull() ?: return@now
                }
            }
        }

        @KetherEditor.Document("ctx origin")
        val main = KetherHelper.simpleKetherNow {
            getEnvironmentContext().origin
        }

    }

    @KetherEditor.Document("ctx sender")
    val sender = KetherHelper.simpleKetherNow {
        getEnvironmentContext().sender
    }


    @KetherEditor.Document("ctx skill ...")
    val skill = object : MultipleKetherParser() {

        @KetherEditor.Document("ctx skill level ...")
        val level = object : MultipleKetherParser() {

            @KetherEditor.Document("ctx skill level to <value:Number>")
            val set = KetherHelper.combinedKetherParser("to") {
                it.group(int()).apply(it) { level ->
                    now {
                        (getEnvironmentContext() as? AbstractSkillContext)?.level = level
                    }
                }
            }

            @KetherEditor.Document("ctx skill level")
            val main = KetherHelper.simpleKetherNow {
                (getEnvironmentContext() as? ImmutableSkillContext)?.level ?: -1
            }

        }

    }

}
