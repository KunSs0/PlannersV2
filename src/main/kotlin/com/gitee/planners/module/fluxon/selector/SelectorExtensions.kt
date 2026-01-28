package com.gitee.planners.module.fluxon.selector

import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.module.fluxon.registerFunction
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.util.Vector
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import kotlin.math.abs

/**
 * 选择器系统扩展
 */
object SelectorExtensions {

    @Awake(LifeCycle.LOAD)
    fun init() {
        val runtime = FluxonScriptCache.runtime

        runtime.registerFunction("selectRectangle", listOf(4)) { ctx ->
            val location = ctx.getRef(0) as? Location ?: return@registerFunction emptyList<Entity>()
            val width = ctx.getAsDouble(1)
            val height = ctx.getAsDouble(2)
            val length = ctx.getAsDouble(3)

            val halfWidth = width / 2.0
            val halfHeight = height / 2.0
            val halfLength = length / 2.0

            location.world?.entities?.filter { entity ->
                val loc = entity.location
                abs(loc.x - location.x) <= halfWidth &&
                abs(loc.y - location.y) <= halfHeight &&
                abs(loc.z - location.z) <= halfLength
            } ?: emptyList<Entity>()
        }

        runtime.registerFunction("selectSphere", listOf(2)) { ctx ->
            val location = ctx.getRef(0) as? Location ?: return@registerFunction emptyList<Entity>()
            val radius = ctx.getAsDouble(1)
            val radiusSquared = radius * radius

            location.world?.entities?.filter { entity ->
                entity.location.distanceSquared(location) <= radiusSquared
            } ?: emptyList<Entity>()
        }

        runtime.registerFunction("selectLine", listOf(2, 3)) { ctx ->
            val location = ctx.getRef(0) as? Location ?: return@registerFunction emptyList<Entity>()
            val distance = ctx.getAsDouble(1)
            val direction = if (ctx.arguments.size > 2) ctx.getRef(2) as? Vector else null

            val dir = direction ?: location.direction
            val normalizedDir = dir.clone().normalize()
            val entities = mutableListOf<Entity>()

            location.world?.entities?.forEach { entity ->
                val toEntity = entity.location.toVector().subtract(location.toVector())
                val projection = toEntity.dot(normalizedDir)

                if (projection in 0.0..distance) {
                    val closestPoint = normalizedDir.clone().multiply(projection)
                    if (toEntity.distance(closestPoint) <= 1.0) {
                        entities.add(entity)
                    }
                }
            }

            entities
        }
    }
}
