package com.gitee.planners.api.pick

/**
 * 以方向角为结果的指向确认结果。
 *
 * @property yaw 水平朝向，单位为度。
 * @property pitch 俯仰角，单位为度。
 */
class PickDirectionResult(
    val yaw: Float,
    val pitch: Float,
) : PickResult {

    override val type = DirectionPickDefinition.TYPE
}
