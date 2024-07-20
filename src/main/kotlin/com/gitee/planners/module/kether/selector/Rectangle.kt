package com.gitee.planners.module.kether.selector

import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.target.Target.Companion.castUnsafely
import com.gitee.planners.api.job.target.TargetBukkitLocation
import com.gitee.planners.api.job.target.TargetLocation
import com.gitee.planners.api.job.target.adaptTarget
import com.gitee.planners.module.kether.getEnvironmentContext
import com.gitee.planners.module.kether.getTargetContainer
import org.bukkit.Location
import org.bukkit.util.Vector
import taboolib.common.platform.function.info
import kotlin.math.absoluteValue

/**
 * 视角前长方形
 * Long 长
 * wide 宽
 * high 高
 * forward 前后偏移
 * offsetY 上下偏移
 * pitch 是否根据pitch改变长方形方向
 *
 * @author zhibeigg
 * @rectangle Long wide high forward offsetY pitch
 */
object Rectangle: AbstractSelector("rec","rectangle") {
    override fun select() = KetherHelper.combinedKetherParser{
        it.group(
            double(),
            double(),
            double(),
            double().option().defaultsTo(0.0),
            double().option().defaultsTo(0.0),
            bool().option().defaultsTo(false)
            ).apply(it) {long, wide, high, forward, offsetY, pitch->
//                val forward = 0.0
//                val offsetY = 0.0
//                val pitch = false

                now {
                    val limit = long + wide + high + forward + offsetY
                    val location = getEnvironmentContext().origin.castUnsafely<TargetLocation<*>>().getBukkitLocation().clone()
                    val entities = location.world?.getNearbyEntities(location, limit, limit, limit) ?: return@now
                    if (!pitch) location.pitch = 0f
                    val vectorX1 = location.clone().direction.normalize()
                    val vectorY1 =
                        if (location.yaw in -360.0..-180.0 || location.yaw in 0.0..180.0) {
                            vectorX1.clone().setZ(0).crossProduct(Vector(0, 0, 1)).normalize()
                        } else {
                            vectorX1.clone().setZ(0).crossProduct(Vector(0, 0, -1)).normalize()
                        }
                    val vectorZ1 = vectorX1.clone().crossProduct(vectorY1.clone()).normalize()

                    val locA = location.clone().add(vectorX1.clone().multiply(forward)).add(vectorY1.clone().multiply(offsetY)).add(vectorZ1.clone().multiply(-(wide / 2)))
                    val locB = location.clone().add(vectorX1.clone().multiply(forward)).add(vectorY1.clone().multiply(offsetY)).add(vectorZ1.clone().multiply(wide / 2))

                    val locC = location.clone().add(vectorX1.clone().multiply(forward)).add(vectorY1.clone().multiply(offsetY + high)).add(vectorZ1.clone().multiply(-(wide / 2)))
                    val locD = location.clone().add(vectorX1.clone().multiply(forward)).add(vectorY1.clone().multiply(offsetY + high)).add(vectorZ1.clone().multiply(wide / 2))

                    val locAF = locA.clone().add(vectorX1.clone().multiply(long))
                    val locBF = locB.clone().add(vectorX1.clone().multiply(long))
                    val locCF = locC.clone().add(vectorX1.clone().multiply(long))
                    val locDF = locD.clone().add(vectorX1.clone().multiply(long))
                    val arrayloc = arrayOf(locA, locB, locC, locD, locAF, locBF, locCF, locDF)

                    entities.forEach { le ->
                        if (isPointInsideCuboid(le.location, arrayloc)){
                            this.getTargetContainer() += le.adaptTarget()
                        }
                    }
                }
        }
    }

    fun isPointInsideCuboid(p: Location, corners: Array<Location>): Boolean {
        val xs = corners.map { it.x }
        val ys = corners.map { it.y }
        val zs = corners.map { it.z }

        val xsm = xs.minOrNull()!!
        val xsb = xs.maxOrNull()!!
        val ysm = ys.minOrNull()!!
        val ysb = ys.maxOrNull()!!
        val zsm = zs.minOrNull()!!
        val zsb = zs.maxOrNull()!!

        return within(p.x, xsm, xsb) && within(p.y, ysm, ysb) && within(p.z, zsm, zsb)
    }
    private fun within(v: Double, bound1: Double, bound2: Double): Boolean =
        v in minOf(bound1, bound2)..maxOf(bound1, bound2)
}