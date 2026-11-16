package com.gitee.planners.module.action

import com.gitee.planners.api.common.util.RectNearestEntityFinder
import org.bukkit.Location
import org.bukkit.entity.Entity
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 会话捕捉的移动包络几何。
 *
 * 每次采样只补查“上一采样点到当前采样点”扫过的体积：把两点位移投影到朝向轴后放大盒体，
 * 中点取两点中点，前后偏移取两次偏移的均值。这样即使玩家单帧位移超过盒体长度也不会穿透。
 */
object CaptureEnvelope {

    /**
     * 构造一次采样的包络盒查找器。
     *
     * @param previous 上一次采样位置。
     * @param current 本次采样位置。
     * @param yaw 采样时刻的身体朝向。
     * @param dimensions 盒体 full 尺寸：宽、高、长。
     * @param offsetFrom 上一次采样的中心偏移。
     * @param offsetTo 本次采样的中心偏移。
     * @param samples 待筛选实体。
     * @return 覆盖整段位移的矩形查找器。
     */
    fun finderFor(
        previous: Location,
        current: Location,
        yaw: Float,
        dimensions: DoubleArray,
        offsetFrom: DoubleArray,
        offsetTo: DoubleArray,
        samples: List<Entity>,
    ): RectNearestEntityFinder {
        val radians = Math.toRadians(yaw.toDouble())
        val cosine = cos(radians)
        val sine = sin(radians)
        val deltaX = current.x - previous.x
        val deltaY = current.y - previous.y
        val deltaZ = current.z - previous.z
        // 位移投影到朝向局部轴：右方与前方分量各算一份膨胀量。
        val right = deltaX * cosine + deltaZ * sine
        val forward = -deltaX * sine + deltaZ * cosine + offsetTo[2] - offsetFrom[2]
        val middle = Location(
            previous.world,
            previous.x + deltaX / 2.0,
            previous.y + deltaY / 2.0,
            previous.z + deltaZ / 2.0,
        )
        return RectNearestEntityFinder(
            middle,
            dimensions[0] + abs(right),
            dimensions[1] + abs(deltaY),
            dimensions[2] + abs(forward),
            yaw,
            (offsetFrom[0] + offsetTo[0]) / 2.0,
            (offsetFrom[1] + offsetTo[1]) / 2.0,
            (offsetFrom[2] + offsetTo[2]) / 2.0,
            samples,
        )
    }
}
