package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.target.Target.Companion.castUnsafely
import com.gitee.planners.api.job.target.TargetLocation
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.kether.getEnvironmentContext
import com.gitee.planners.module.kether.getTargetContainer
import org.bukkit.util.Vector
import taboolib.common5.cfloat
import java.lang.Math
import kotlin.math.cos
import kotlin.math.sin

object OriginOffset: AbstractSelector("offset", "os") {

    override fun select() = KetherHelper.combinedKetherParser { instance ->
        instance.group(
            double(),
            double(),
            double(),
            double().option().defaultsTo(null),
            double().option().defaultsTo(null)
        ).apply(instance) { x,y,z,yaw,pitch ->
            now {
                val location = getEnvironmentContext().origin.castUnsafely<TargetLocation<*>>().getBukkitLocation().clone()
                if (yaw != null) {
                    val rotateYaw = location.yaw+yaw

                    val rotatePitch = if (pitch != null) location.pitch+pitch else 0.0

                    val vec = Vector(x,y,z).rotateAroundYawPitch(rotateYaw.cfloat,rotatePitch.cfloat)
                    location.add(vec)
                    this.getTargetContainer() += location.adaptTarget()
                } else {
                    val offset = Vector(x,y,z).toLocation(location.world!!,location.yaw,location.pitch)
                    location.add(offset)
                    this.getTargetContainer() += location.adaptTarget()
                }
            }
        }
    }


    private fun Vector.rotateAroundYawPitch(yawDegrees: Float, pitchDegrees: Float): Vector {
        val yaw = Math.toRadians((-1 * (yawDegrees + 90)).toDouble())
        val pitch = Math.toRadians(-pitchDegrees.toDouble())
        val cosYaw = cos(yaw)
        val cosPitch = cos(pitch)
        val sinYaw = sin(yaw)
        val sinPitch = sin(pitch)

        // Z_Axis rotation (Pitch)
        var initialX: Double = this.x
        val initialY: Double = this.y
        var x = initialX * cosPitch - initialY * sinPitch
        val y = initialX * sinPitch + initialY * cosPitch

        // Y_Axis rotation (Yaw)
        val initialZ: Double = this.z
        initialX = x
        val z = initialZ * cosYaw - initialX * sinYaw
        x = initialZ * sinYaw + initialX * cosYaw
        return Vector(x, y, z)
    }
}