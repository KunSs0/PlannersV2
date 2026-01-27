package com.gitee.planners.core.skill.entity.animated

import com.gitee.planners.api.job.target.ProxyTarget
import org.bukkit.entity.Entity

interface EntitySpawner {

    fun create(target: ProxyTarget<*>): Entity

}
