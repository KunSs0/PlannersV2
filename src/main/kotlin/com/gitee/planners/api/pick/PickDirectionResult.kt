package com.gitee.planners.api.pick

/**
 * 以方向为结果的指向确认结果。
 *
 * @property x 方向 X 分量。
 * @property y 方向 Y 分量。
 * @property z 方向 Z 分量。
 */
class PickDirectionResult(
    val x: Double,
    val y: Double,
    val z: Double,
) : PickResult {

    override val type = DirectionPickDefinition.TYPE
}
