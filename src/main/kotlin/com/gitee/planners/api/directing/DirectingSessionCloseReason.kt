package com.gitee.planners.api.directing

/**
 * 指向会话结束原因。
 */
enum class DirectingSessionCloseReason {
    CONFIRMED,
    CANCELLED,
    TIMED_OUT,
    REPLACED,
    PLAYER_QUIT,
    PLUGIN_DISABLED,
}
