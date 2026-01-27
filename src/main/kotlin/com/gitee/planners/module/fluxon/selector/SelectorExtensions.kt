package com.gitee.planners.module.fluxon.selector

import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.util.Vector
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 选择器系统扩展
 */
object SelectorExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime

        // selectRectangle(location, width, height, length) -> List<Entity>
        runtime.registerFunction("selectRectangle", FunctionSignature.returns(Type.OBJECT).params(Type.OBJECT, Type.D, Type.D, Type.D)) { ctx ->
            val location = ctx.getRef(0) as? Location ?: return@registerFunction
            val width = ctx.getAsDouble(1)
            val height = ctx.getAsDouble(2)
            val length = ctx.getAsDouble(3)

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

        // selectSphere(location, radius) -> List<Entity>
        runtime.registerFunction("selectSphere", FunctionSignature.returns(Type.OBJECT).params(Type.OBJECT, Type.D)) { ctx ->
            val location = ctx.getRef(0) as? Location ?: return@registerFunction
            val radius = ctx.getAsDouble(1)
            val radiusSquared = radius * radius

            val entities = location.world?.entities?.filter { entity ->
                entity.location.distanceSquared(location) <= radiusSquared
            } ?: emptyList()

            ctx.setReturnRef(entities)
        }

        // selectLine(location, distance, direction) -> List<Entity>
        runtime.registerFunction("selectLine", FunctionSignature.returns(Type.OBJECT).params(Type.OBJECT, Type.D, Type.OBJECT)) { ctx ->
            val location = ctx.getRef(0) as? Location ?: return@registerFunction
            val distance = ctx.getAsDouble(1)
            val direction = ctx.getRef(2) as? Vector ?: location.direction

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

        // selectLine(location, distance) -> List<Entity> (使用location的direction)
        runtime.registerFunction("selectLine", FunctionSignature.returns(Type.OBJECT).params(Type.OBJECT, Type.D)) { ctx ->
            val location = ctx.getRef(0) as? Location ?: return@registerFunction
            val distance = ctx.getAsDouble(1)
            val direction = location.direction

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
