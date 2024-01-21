package com.gitee.planners.api.common.util

import org.bukkit.Location
import org.bukkit.entity.Entity
import taboolib.module.navigation.BoundingBox

class NearestEntityFinder(val origin: Location) {

    val world = origin.world!!

    val center = origin.toVector()

    fun request(): List<Entity> {
        val boundingBoxes = world.entities.map { entity ->
            val boundingBoxWidth = entity.width
            val boundingBoxHeight = entity.height
            val x = entity.location.x
            val y = entity.location.y
            val z = entity.location.z
            entity to BoundingBox(
                x - boundingBoxWidth, y - boundingBoxHeight, z - boundingBoxWidth,
                x + boundingBoxWidth, y + boundingBoxHeight, z + boundingBoxWidth,
            )
        }

        return boundingBoxes.filter { it.second.contains(center) }.map { it.first }
    }


}
