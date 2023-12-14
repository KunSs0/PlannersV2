package com.gitee.planners.api.common.entity

import org.bukkit.entity.Entity

class ProxyBukkitEntity(val instance: Entity) : ProxyEntity<Entity> {

    override fun getInstance(): Entity {
        return instance
    }

    override fun isDead(): Boolean {
        return getInstance().isDead
    }

}