package com.gitee.planners.module.action

/**
 * 一个动作的接收目标。
 *
 * 目标可以是玩家、实体或世界坐标；只有持有具体句柄的目标才能参与
 * 存活判断，位置目标只判断世界是否存在。
 */
sealed class ActionTarget {

    /**
     * 玩家目标。
     *
     * @property handle 玩家实例。
     */
    class PlayerTarget(val handle: org.bukkit.entity.Player) : ActionTarget() {

        override fun isActive(): Boolean {
            return handle.isOnline
        }

        override fun bukkitEntity(): org.bukkit.entity.LivingEntity? {
            return handle
        }
    }

    /**
     * 实体目标。
     *
     * @property handle 实体实例。
     */
    class EntityTarget(val handle: org.bukkit.entity.LivingEntity) : ActionTarget() {

        override fun isActive(): Boolean {
            return handle.isValid && !handle.isDead
        }

        override fun bukkitEntity(): org.bukkit.entity.LivingEntity? {
            return handle
        }
    }

    /**
     * 世界坐标目标。
     *
     * @property location 坐标。
     */
    class LocationTarget(val location: org.bukkit.Location) : ActionTarget() {

        override fun isActive(): Boolean {
            return location.world != null
        }

        override fun bukkitEntity(): org.bukkit.entity.LivingEntity? {
            return null
        }
    }

    /**
     * 目标仍然有效时返回 true。
     *
     * @return 有效时返回 true。
     */
    abstract fun isActive(): Boolean

    /**
     * 目标对应的 Bukkit 实体；位置目标返回 null。
     *
     * @return 实体或 null。
     */
    abstract fun bukkitEntity(): org.bukkit.entity.LivingEntity?

    /**
     * 目标对应的玩家；非玩家目标返回 null。
     *
     * @return 玩家或 null。
     */
    fun bukkitPlayer(): org.bukkit.entity.Player? {
        val entity = bukkitEntity()
        if (entity is org.bukkit.entity.Player) {
            return entity
        }
        return null
    }
}
