package com.gitee.planners.core.skill.entity.animated

import com.gitee.planners.api.common.entity.animated.Animated
import com.gitee.planners.api.common.entity.animated.AnimatedMeta
import com.gitee.planners.api.common.task.SimpleUniqueTask
import com.gitee.planners.api.job.target.ProxyTarget
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import taboolib.platform.util.setMeta

/**
 * 实体构建器
 */
class BukkitEntityBuilder(val type: EntityType, override val timestampTick: Long) : AbstractBukkitEntityAnimated<Entity>(),
    EntitySpawner,Animated.Periodic {

    /** 存活期 单位tick */
    val validityTick = long("validity", timestampTick) {

    }

    /** 障碍 （launch模式下有效） */
    val isObstacle = bool("is-obstacle",false) {

    }

    override fun create(target: ProxyTarget<*>): Entity {
        val location = (target as ProxyTarget.Location<*>).getBukkitLocation()
        val entity = location.world!!.spawnEntity(location, type)
        // 创建删除任务
        SimpleUniqueTask.create(entity,timestampTick,false) {
            entity.remove()
        }
        return entity
    }



    fun getClearableTask(entity: Entity): SimpleUniqueTask? {
        return SimpleUniqueTask.getTask(entity)
    }


}
