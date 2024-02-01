package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.job.selector.Selector
import com.gitee.planners.api.job.target.TargetEntity
import com.gitee.planners.module.kether.getTargetContainer
import taboolib.library.kether.QuestActionParser
import taboolib.module.kether.combinationParser


object Amount : Selector {
    override fun namespace(): Array<String> = arrayOf("amount", "count", "limit")

    override fun action(): QuestActionParser {
        return combinationParser { instance ->
            instance.group(int()).apply(instance) { amount ->
                now {
                    getTargetContainer().removeIf { it !is TargetEntity<*> }
                    // Remove entities until the amount is reached
                    while (getTargetContainer().size > amount) {
                        getTargetContainer().removeAt(0)
                    }
                }
            }
        }
    }


}