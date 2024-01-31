package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.job.selector.Selector
import com.gitee.planners.api.job.target.Target.Companion.cast
import com.gitee.planners.api.job.target.TargetLocation
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.kether.getEnvironmentContext
import com.gitee.planners.module.kether.getTargetContainer
import taboolib.common.util.Vector
import taboolib.library.kether.QuestActionParser
import taboolib.module.kether.combinationParser

object Range : Selector {

    override fun namespace(): Array<String> {
        return arrayOf("range")
    }

    override fun action(): QuestActionParser {
        return combinationParser {
            it.group(double()).apply(it) { r ->
                now {
                    val vector = Vector(r, r, r)
                    val entities = getEnvironmentContext().origin.cast<TargetLocation<*>>().getNearbyLivingEntities(vector)
                    this.getTargetContainer() += entities.map { it.adaptTarget() }
                }
            }
        }
    }
}
