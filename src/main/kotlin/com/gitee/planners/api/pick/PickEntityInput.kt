package com.gitee.planners.api.pick

import java.util.UUID

/**
 * 实体指向的客户端上报快照。
 *
 * 客户端只上报选中实体与相机朝向；服务端据此复核距离、夹角与视线，不信任客户端判断。
 *
 * @property sessionId 会话 ID。
 * @property entityId 客户端选中的实体唯一 ID。
 * @property aimYaw 上报时的相机水平朝向，单位为度。
 * @property aimPitch 上报时的相机俯仰角，单位为度。
 */
class PickEntityInput(
    override val sessionId: Long,
    val entityId: UUID,
    val aimYaw: Float,
    val aimPitch: Float,
) : PickInput {

    override val type = EntityPickDefinition.TYPE
}
