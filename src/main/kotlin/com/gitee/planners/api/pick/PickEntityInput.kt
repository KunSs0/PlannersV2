package com.gitee.planners.api.pick

import java.util.UUID

/**
 * 实体指向的客户端上报快照。
 *
 * 客户端只上报选中实体与相机朝向；服务端据此复核距离、夹角与视线，不信任客户端判断。
 * 没有命中任何实体时仍要上报一次，实体 ID 为空，由服务端按定义的兜底策略决定后续。
 *
 * @property sessionId 会话 ID。
 * @property entityId 客户端选中的实体唯一 ID；null 表示本次指向没有命中实体。
 * @property aimX 上报时相机前向的 X 分量。
 * @property aimY 上报时相机前向的 Y 分量。
 * @property aimZ 上报时相机前向的 Z 分量。
 */
class PickEntityInput(
    override val sessionId: Long,
    val entityId: UUID?,
    val aimX: Double,
    val aimY: Double,
    val aimZ: Double,
) : PickInput {

    override val type = EntityPickDefinition.TYPE
}
