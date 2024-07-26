package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.target.Target.Companion.castUnsafely
import com.gitee.planners.api.job.target.TargetLocation
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.kether.getEnvironmentContext
import com.gitee.planners.module.kether.getTargetContainer
import org.bukkit.Location
import taboolib.common.util.Vector
import taboolib.common5.cdouble
import kotlin.math.cos
import kotlin.math.sin


/**
 *  半径
 *  高度
 *  旋转角度
 *  原视角继承
 * at @cd radius y angle keepVisual(true)
 * at @cd 2
 */
object CircleDot : AbstractSelector("c-dot", "cd") {

    override fun select() = KetherHelper.combinedKetherParser {
        it.group(
            double(),
            double().option().defaultsTo(0.0),
            double().option().defaultsTo(0.0),
            bool().option().defaultsTo(true)
        ).apply(it) { r,y,angle,keepVisual ->
            now {
                val location = getEnvironmentContext().origin.castUnsafely<TargetLocation<*>>().getBukkitLocation().clone()
                val radians = Math.toRadians(angle+location.yaw)
                val x: Double = r * cos(radians)
                val z: Double = r * sin(radians)

                location.pitch = 0f

                val newloc = rotateLocationAboutPoint(location.add(x, y, z), location.yaw.cdouble, location)

                if (keepVisual) {
                    newloc.pitch = location.pitch
                    newloc.yaw = location.yaw
                }
                this.getTargetContainer() += newloc.adaptTarget()
            }
        }
    }
    private fun rotateLocationAboutPoint(location: Location, angle: Double, point: Location): Location {
        val radians = Math.toRadians(angle)
        val dx = location.x - point.x
        val dz = location.z - point.z
        val newX = dx * cos(radians) - dz * sin(radians) + point.x
        val newZ = dz * cos(radians) + dx * sin(radians) + point.z
        return Location(location.world, newX, location.y, newZ)
    }


}