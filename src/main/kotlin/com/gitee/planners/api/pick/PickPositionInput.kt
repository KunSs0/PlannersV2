package com.gitee.planners.api.pick

/**
 * 位置指向的客户端上报快照。
 *
 * @property sessionId 会话 ID。
 * @property x 落点 X。
 * @property y 落点 Y。
 * @property z 落点 Z。
 */
class PickPositionInput(
    override val sessionId: Long,
    val x: Double,
    val y: Double,
    val z: Double,
) : PickInput {

    override val type = PositionPickDefinition.TYPE
}
