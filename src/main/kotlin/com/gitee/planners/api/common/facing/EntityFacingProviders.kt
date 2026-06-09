package com.gitee.planners.api.common.facing

import org.bukkit.entity.LivingEntity

object EntityFacingProviders {

    @Volatile
    private var provider: EntityFacingProvider = BukkitLocationYawFacingProvider

    @JvmStatic
    fun register(provider: EntityFacingProvider) {
        this.provider = provider
    }

    @JvmStatic
    fun getFacingYaw(entity: LivingEntity): Float {
        return provider.getFacingYaw(entity)
    }
}
