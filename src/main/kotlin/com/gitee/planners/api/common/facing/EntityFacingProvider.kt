package com.gitee.planners.api.common.facing

import org.bukkit.entity.LivingEntity

interface EntityFacingProvider {

    fun getFacingYaw(entity: LivingEntity): Float
}
