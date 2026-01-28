package com.gitee.planners.module.fluxon.selector

import com.gitee.planners.module.fluxon.FluxonScriptCache
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.util.Vector
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import org.tabooproject.fluxon.runtime.java.Export
import org.tabooproject.fluxon.runtime.java.Optional
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import kotlin.math.abs

/**
 * 选择器系统扩展
 */
object SelectorExtensions {

    @Awake(LifeCycle.LOAD)
    private fun init() {
        val runtime = FluxonScriptCache.runtime
        runtime.registerFunction("pl:selector", "selector", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
            ctx.setReturnRef(SelectorObject)
        }
        runtime.exportRegistry.registerClass(SelectorObject::class.java, "pl:selector")
    }

    object SelectorObject {

        @JvmField
        val TYPE: Type = Type.fromClass(SelectorObject::class.java)

        @Export
        fun rectangle(location: Location, width: Double, height: Double, length: Double): List<Entity> {
            val halfWidth = width / 2.0
            val halfHeight = height / 2.0
            val halfLength = length / 2.0

            return location.world?.entities?.filter { entity ->
                val loc = entity.location
                abs(loc.x - location.x) <= halfWidth &&
                abs(loc.y - location.y) <= halfHeight &&
                abs(loc.z - location.z) <= halfLength
            } ?: emptyList()
        }

        @Export
        fun sphere(location: Location, radius: Double): List<Entity> {
            val radiusSquared = radius * radius
            return location.world?.entities?.filter { entity ->
                entity.location.distanceSquared(location) <= radiusSquared
            } ?: emptyList()
        }

        @Export
        fun line(location: Location, distance: Double, @Optional direction: Vector?): List<Entity> {
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

            return entities
        }
    }
}
