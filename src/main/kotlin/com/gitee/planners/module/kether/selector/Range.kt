package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.script.kether.SimpleKetherParser
import com.gitee.planners.api.job.selector.Selector
import com.gitee.planners.api.job.target.Target.Companion.castUnsafely
import com.gitee.planners.api.job.target.TargetLocation
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.kether.getEnvironmentContext
import com.gitee.planners.module.kether.getTargetContainer
import taboolib.common.util.Vector
import taboolib.module.kether.combinationParser

object Range : Selector {

    override fun namespace(): Array<String> {
        return arrayOf("range")
    }

    override fun action() = KetherHelper.combinedKetherParser {
        it.group(double()).apply(it) { r ->
            now {
                val vector = Vector(r, r, r)
                val entities = getEnvironmentContext().origin.castUnsafely<TargetLocation<*>>().getNearbyLivingEntities(vector)
                this.getTargetContainer() += entities.map { it.adaptTarget() }
            }
        }
    }
}
