package com.gitee.planners.module.fluxon.selector

import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.util.Vector
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type

/**
 * 选择器系统扩展
 * 通过 Location 扩展实现选择器功能
 */
object SelectorExtensions {

    fun register() {
        val runtime = FluxonScriptCache.runtime

        // Location 选择器扩展
        runtime.registerExtension(Location::class.java)
            .function("selectRectangle", FunctionSignature.returns(Type.OBJECT).params(Type.D, Type.D, Type.D)) { ctx ->
                val location = ctx.target ?: return@function
                val width = ctx.getAsDouble(0)
                val height = ctx.getAsDouble(1)
                val length = ctx.getAsDouble(2)

                val halfWidth = width / 2.0
                val halfHeight = height / 2.0
                val halfLength = length / 2.0

                val entities = location.world?.entities?.filter { entity ->
                    val loc = entity.location
                    val dx = kotlin.math.abs(loc.x - location.x)
                    val dy = kotlin.math.abs(loc.y - location.y)
                    val dz = kotlin.math.abs(loc.z - location.z)
                    dx <= halfWidth && dy <= halfHeight && dz <= halfLength
                } ?: emptyList()

                ctx.setReturnRef(entities)
            }
            .function("selectSphere", FunctionSignature.returns(Type.OBJECT).params(Type.D)) { ctx ->
                val location = ctx.target ?: return@function
                val radius = ctx.getAsDouble(0)
                val radiusSquared = radius * radius

                val entities = location.world?.entities?.filter { entity ->
                    entity.location.distanceSquared(location) <= radiusSquared
                } ?: emptyList()

                ctx.setReturnRef(entities)
            }
            .function("selectLine", FunctionSignature.returns(Type.OBJECT).params(Type.D, Type.OBJECT)) { ctx ->
                val location = ctx.target ?: return@function
                val distance = ctx.getAsDouble(0)
                val direction = ctx.getRef(1) as? Vector ?: location.direction

                val normalizedDir = direction.clone().normalize()
                val entities = mutableListOf<Entity>()

                location.world?.entities?.forEach { entity ->
                    val toEntity = entity.location.toVector().subtract(location.toVector())
                    val projection = toEntity.dot(normalizedDir)

                    if (projection >= 0.0 && projection <= distance) {
                        val closestPoint = normalizedDir.clone().multiply(projection)
                        val distanceToLine = toEntity.distance(closestPoint)

                        if (distanceToLine <= 1.0) {
                            entities.add(entity)
                        }
                    }
                }

                ctx.setReturnRef(entities)
            }
    }
}
