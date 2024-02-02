package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.target.Target.Companion.castUnsafely
import com.gitee.planners.api.job.target.TargetLocation
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.kether.getEnvironmentContext
import com.gitee.planners.module.kether.getTargetContainer
import taboolib.common.util.Vector

object RangeNegate : AbstractSelector("f-range", "not-range") {

    override fun select() = KetherHelper.combinedKetherParser { instance ->
        instance.group(double()).apply(instance) { r ->
            now {
                val vector = Vector(r, r, r)
                val entities = getEnvironmentContext().origin.castUnsafely<TargetLocation<*>>().getNearbyLivingEntities(vector)
                this.getTargetContainer() -= entities.map { it.adaptTarget() }.toSet()
            }
        }
    }

}