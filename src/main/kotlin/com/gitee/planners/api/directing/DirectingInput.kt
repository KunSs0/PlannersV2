package com.gitee.planners.api.directing

/**
 * 指向会话接收的客户端输入。
 *
 * 输入只承载 provider 需要复核的快照，不能代替服务端最终校验。
 */
interface DirectingInput {

    /**
     * 当前指向会话 ID。
     */
    val sessionId: Long

    /**
     * 产生此输入的 provider 类型。
     */
    val type: String
}
