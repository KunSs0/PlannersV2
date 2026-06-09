package com.gitee.planners.api.common.facing

import org.bukkit.entity.LivingEntity

object BukkitLocationYawFacingProvider : EntityFacingProvider {

    override fun getFacingYaw(entity: LivingEntity): Float {
        return entity.location.yaw
    }
}
