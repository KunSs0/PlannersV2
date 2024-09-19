package com.gitee.planners.api.common.entity

import org.bukkit.entity.Entity

class ProxyBukkitEntity(instance: Entity) : ProxyEntity<Entity> {

    val instance = instance
        @JvmName("instance0")
        get

    override fun getInstance(): Entity {
        return instance
    }

    override fun isDead(): Boolean {
        return getInstance().isDead
    }

    override fun hashCode(): Int {
        return instance.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProxyBukkitEntity

        return instance == other.instance
    }


}
