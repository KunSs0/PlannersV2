package com.gitee.planners.api.pick

/**
 * 方向指向的客户端上报快照。
 *
 * 客户端只上报相机前向向量，服务端据此复算俯仰角并校验上限。
 *
 * @property sessionId 会话 ID。
 * @property aimX 相机前向的 X 分量。
 * @property aimY 相机前向的 Y 分量。
 * @property aimZ 相机前向的 Z 分量。
 */
class PickDirectionInput(
    override val sessionId: Long,
    val aimX: Double,
    val aimY: Double,
    val aimZ: Double,
) : PickInput {

    override val type = DirectionPickDefinition.TYPE
}
