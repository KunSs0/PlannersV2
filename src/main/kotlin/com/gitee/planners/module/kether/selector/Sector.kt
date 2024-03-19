package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.common.WorkableTransform
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.common.util.SectorNearestEntityFinder
import com.gitee.planners.api.job.target.TargetLocation
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.kether.actionDouble
import com.gitee.planners.module.kether.commandObjectiveOrOrigin
import com.gitee.planners.module.kether.getTargetContainer
import com.gitee.planners.util.syncing

object Sector : AbstractSelector("sector") {

    override fun select() = KetherHelper.combinedKetherParser {
        it.group(actionDouble(), actionDouble(), command("yaw", then = text()).option(),commandObjectiveOrOrigin()).apply(it) { radius, angle, yaw,objective ->
            val origin = objective.filterIsInstance<TargetLocation<*>>().firstOrNull()?.getBukkitLocation()?.clone()
            now {
                if (origin == null) return@now
                origin.yaw = if (yaw != null) {
                    WorkableTransform.buildFloat(yaw) { origin.yaw }
                } else {
                    origin.yaw
                }
                val sampling = syncing { origin.world!!.entities }
                try {
                    getTargetContainer() += SectorNearestEntityFinder(origin,angle,radius,origin.yaw, sampling.get()).request().map { it.adaptTarget() }
                }catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

}
