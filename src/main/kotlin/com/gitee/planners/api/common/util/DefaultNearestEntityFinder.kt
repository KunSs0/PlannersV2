package com.gitee.planners.api.common.util

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import taboolib.module.navigation.BoundingBox

class DefaultNearestEntityFinder(origin: Location, samples: List<Entity>) : NearestEntityFinder(origin, samples) {

    private val center = origin.toVector()

    override fun request() : List<Entity> {
        val boundingBoxes = samples.map { entity -> entity to getBoundingBox(entity) }
        return boundingBoxes.filter { it.second.contains(center) }.map { it.first }
    }

}
