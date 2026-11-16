package com.gitee.planners.api.pick

/**
 * 指向表现后端。
 *
 * Planners 只负责类型、会话与结果校验；瞄准界面、拾取、预览与按键确认由实现方负责。
 * 实现通常由脚本注册：开启时向客户端下发会话参数，结束后清理，并把客户端上报接回
 * [PickAPI.confirm] / [PickAPI.cancel]。
 */
interface PickPresentation {

    /**
     * 开启一次表现。
     *
     * @param context 本次会话的固定上下文。
     */
    fun open(context: PickContext)

    /**
     * 结束一次表现。
     *
     * @param sessionId 会话 ID。
     * @param reason 会话结束原因。
     */
    fun close(sessionId: Long, reason: PickCloseReason)
}
