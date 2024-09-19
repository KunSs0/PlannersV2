package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.api.job.target.TargetContainerization
import com.gitee.planners.module.kether.commandInt
import com.gitee.planners.module.kether.commandObjectiveOrOrigin
import com.gitee.planners.module.kether.getTargetContainer

/**
 * 保留存活实体
 * remain-living
 */
object RemainLiving : AbstractSelector("remain-living") {

    override fun select() = KetherHelper.simpleKetherNow {
        this.getTargetContainer().removeIf {
            if (it is TargetBukkitEntity) {
                it.instance.isDead
            } else {
                false
            }
        }
    }
}
