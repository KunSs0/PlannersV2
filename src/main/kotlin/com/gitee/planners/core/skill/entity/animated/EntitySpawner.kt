package com.gitee.planners.core.skill.entity.animated

import com.gitee.planners.api.job.target.Target
import org.bukkit.entity.Entity

interface EntitySpawner {

    fun create(target: Target<*>): Entity

}
