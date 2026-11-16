package com.gitee.planners.api.pick

/**
 * 指向会话结束原因。
 */
enum class PickCloseReason {
    CONFIRMED,
    CANCELLED,
    TIMED_OUT,
    REPLACED,
    PLAYER_QUIT,
    PLUGIN_DISABLED,
}
