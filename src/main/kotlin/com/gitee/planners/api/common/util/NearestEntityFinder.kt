package com.gitee.planners.api.common.util

import com.gitee.planners.api.common.registry.defaultRegistry
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import taboolib.common.platform.Schedule
import taboolib.module.navigation.BoundingBox
import java.util.Collections

abstract class NearestEntityFinder(val origin: Location, var samples: List<Entity>) {

    constructor(origin: Location,sampling: SynchronousSampling<List<Entity>>) : this(origin,sampling.get())

    abstract fun request(): List<Entity>

    fun getBoundingBox(entity: Entity): BoundingBox {
        return cache.computeIfAbsent(entity) {
            val boundingBoxWidth = entity.width
            val boundingBoxHeight = entity.height
            val x = entity.location.x
            val y = entity.location.y
            val z = entity.location.z
            BoundingBox(
                x - boundingBoxWidth, y - boundingBoxHeight, z - boundingBoxWidth,
                x + boundingBoxWidth, y + boundingBoxHeight, z + boundingBoxWidth,
            )
        }
    }

    companion object {

        private val cache = Collections.synchronizedMap(mutableMapOf<Entity,BoundingBox>())

        @Schedule(async = true, period = 20 * 60 * 60)
        private fun clear() {
            val iterator = cache.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (next.key.isDead || next.key.isEmpty) {
                    iterator.remove()
                }
            }
        }

    }

}
