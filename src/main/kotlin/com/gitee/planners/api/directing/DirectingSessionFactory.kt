package com.gitee.planners.api.directing

/**
 * provider 类型对应的指向会话创建器。
 */
interface DirectingSessionFactory {

    /**
     * 支持的 provider 类型。
     */
    val type: String

    /**
     * 创建一次新的指向会话。
     *
     * @param context 本次施法的固定上下文。
     * @return 新建的 provider 会话。
     */
    fun create(context: DirectingSessionContext): DirectingSession
}
