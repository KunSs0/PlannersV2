package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.job.selector.Selector
import com.gitee.planners.api.job.target.Target.Companion.castUnsafely
import com.gitee.planners.api.job.target.TargetLocation
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.kether.getEnvironmentContext
import com.gitee.planners.module.kether.getTargetContainer
import taboolib.common.util.Vector
import taboolib.library.kether.QuestActionParser
import taboolib.module.kether.combinationParser

object NegateRange : Selector {

    override fun namespace(): Array<String> = arrayOf("f-range", "!range", "not-type")

    override fun action(): QuestActionParser {
        return combinationParser { instance ->
            instance.group(double()).apply(instance) { r ->
                now {
                    val vector = Vector(r, r, r)
                    val entities = getEnvironmentContext().origin.castUnsafely<TargetLocation<*>>().getNearbyLivingEntities(vector)
                    this.getTargetContainer() -= entities.map { it.adaptTarget() }.toSet()
                }
            }
        }
    }
}