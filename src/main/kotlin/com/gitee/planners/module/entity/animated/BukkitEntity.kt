package com.gitee.planners.module.entity.animated

import com.gitee.planners.api.common.entity.animated.Animated
import com.gitee.planners.api.common.task.SimpleUniqueFutureTask
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.TargetLocation
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import java.util.*

class BukkitEntity(val type: EntityType,validityTick: Int) : AbstractBukkitEntityAnimated<Entity>(),Animated.Periodic {

    val isGravity = bool("is-gravity", true) {
        instance.setGravity(it)
    }

    /** 存活期 单位tick */
    val validityTick = int("validity", validityTick) {

    }

    /** 障碍 （launch模式下有效） */
    val isObstacle = bool("is-obstacle",false) {

    }

    override val timestampTick: Long
        get() = validityTick.asLong()

    override fun create(target: Target<*>): Entity {
        val location = (target as TargetLocation<*>).getBukkitLocation()
        val entity = location.world!!.spawnEntity(location, type)
        // 创建删除任务
        SimpleUniqueFutureTask.create(entity,timestampTick,true) {
            entity.remove()
        }
        return entity
    }

    fun getClearableTask(entity: Entity): SimpleUniqueFutureTask? {
        return SimpleUniqueFutureTask.getTask(entity)
    }


}
