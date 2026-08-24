package com.gitee.planners.api.directing

/**
 * 单次指向技能的运行时会话。
 *
 * 会话由 Planners 创建和停止；provider 只负责处理自己的瞄准输入、服务端校验与表现资源。
 */
interface DirectingSession {

    /**
     * 会话开始后调用一次。
     */
    fun begin()

    /**
     * 接收瞄准期间的新快照。
     *
     * @param input 客户端上传的 provider 专属快照。
     */
    fun update(input: DirectingInput)

    /**
     * 根据最终快照确认本次指向。
     *
     * @param input 松开按键时的 provider 专属快照。
     * @return 服务端校验成功后的指向结果；校验失败时返回 null。
     */
    fun confirm(input: DirectingInput): DirectingResult?

    /**
     * 结束会话并释放 provider 持有的临时资源。
     *
     * @param reason 会话结束原因。
     */
    fun close(reason: DirectingSessionCloseReason)
}
