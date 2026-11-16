package com.gitee.planners.api.pick

/**
 * 实体指向的客户端上报快照。
 *
 * @property sessionId 会话 ID。
 * @property entityId 客户端选中的实体 ID。
 */
class PickEntityInput(
    override val sessionId: Long,
    val entityId: Int,
) : PickInput {

    override val type = EntityPickDefinition.TYPE
}
