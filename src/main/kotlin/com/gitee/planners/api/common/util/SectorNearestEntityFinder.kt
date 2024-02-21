package com.gitee.planners.api.common.util

import com.gitee.planners.module.kether.selector.Sector
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.util.Vector
import taboolib.common5.cfloat
import taboolib.module.navigation.BoundingBox
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

class SectorNearestEntityFinder(origin: Location,val angle: Double,val radius: Double,val direction: Float, samples: List<Entity>) : NearestEntityFinder(origin, samples) {

    override fun request(): List<Entity> {
        return samples.filter { isBoundingBoxInSector(getBoundingBox(it)) }
    }

    fun isBoundingBoxInSector(boundingBox: BoundingBox): Boolean {
        val center = boundingBox.getCenter()
        val centerAB = Vector(center.x - this.origin.x, center.y - this.origin.y, 0.0)
        val distanceAB = hypot(centerAB.x, centerAB.y)
        if (distanceAB > this.radius) {
            return false
        }
        // 计算向量的角度
        val angle = Math.toDegrees(atan2(centerAB.y, centerAB.x))

        // 将扇形方向转换为与X轴的角度差
        val difference = normalizeYaw(angle.cfloat - this.direction)

        // 如果矩形中心与扇形中心的角度差在扇形开角度的一半之内，则认为碰撞
        return abs(difference) <= this.angle / 2
    }

    @Suppress("NAME_SHADOWING")
    fun normalizeYaw(yaw: Float): Float {
        var yaw = yaw
        yaw %= 360.0f
        if (yaw >= 180.0f) {
            yaw -= 360.0f
        } else if (yaw < -180.0f) {
            yaw += 360.0f
        }
        return yaw
    }
    fun BoundingBox.getCenter(): Vector {
        return Vector((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2)
    }
}
