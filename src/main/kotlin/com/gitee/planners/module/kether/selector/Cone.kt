package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.target.Target.Companion.cast
import com.gitee.planners.api.job.target.TargetEntity
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.kether.getEnvironmentContext
import com.gitee.planners.module.kether.getTargetContainer
import java.lang.Math.toRadians
import kotlin.math.PI
import taboolib.common.util.Vector as TVector
import kotlin.math.atan2
import kotlin.math.sqrt

object Cone: AbstractSelector("cone", "sec"){

    override fun select() = KetherHelper.combinedKetherParser {
        it.group(double(), double(), double().option().defaultsTo(0.0)).apply(it) { r, angle,exAndge ->

            now{
                val origin = getEnvironmentContext().origin.cast<TargetEntity<*>>() ?: error("origin is not a entity")
                val entities = origin.getNearbyLivingEntities(TVector(r,r,r))

                val x = origin.getX()
                val y = origin.getY()
                val z = origin.getZ()
                val yaw = origin.getBukkitEyeLocation().yaw + exAndge

                this.getTargetContainer() += entities.filter { entity ->
                    if (origin.instance == entity) return@filter false
                    val location = entity.location
                    val dx = location.x - x
                    val dz = location.z - z
                    val distance = sqrt(dx * dx + dz * dz)
                    val angleToEntity =  atan2(dz,dx) - toRadians(yaw)
                    val normalizedAngle = ((angleToEntity + PI) % (PI * 2)) - PI

                    return@filter distance <= r && normalizedAngle >= -angle/2 && normalizedAngle <= angle/2
                }.map { it.adaptTarget() }
            }
        }
    }
//    private fun dotProduct(v1: Vector, v2: Vector) = v1.x * v2.x + v1.y * v2.y + v1.z * v2.z

}