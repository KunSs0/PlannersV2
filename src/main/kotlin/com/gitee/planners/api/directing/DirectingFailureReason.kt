package com.gitee.planners.api.directing

/**
 * 指向失败原因。
 *
 * 只描述"这次选点没有得出结论"，不包含玩家主动取消、被新会话替换或离线这几种非失败场景。
 */
enum class DirectingFailureReason {

    /**
     * provider 只有配置解析、没有注册运行期会话创建器。
     */
    NO_FACTORY,

    /**
     * 会话确认时判定落点或目标非法（超距、无落点、被遮挡等）。
     */
    INVALID_TARGET,

    /**
     * 超过配置的最长指向时长仍未确认。
     */
    TIMEOUT,
}
