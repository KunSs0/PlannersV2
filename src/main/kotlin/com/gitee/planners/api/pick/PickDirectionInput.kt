package com.gitee.planners.api.pick

/**
 * 方向指向的客户端上报快照。
 *
 * @property sessionId 会话 ID。
 * @property yaw 水平朝向，单位为度。
 * @property pitch 俯仰角，单位为度。
 */
class PickDirectionInput(
    override val sessionId: Long,
    val yaw: Float,
    val pitch: Float,
) : PickInput {

    override val type = DirectionPickDefinition.TYPE
}
