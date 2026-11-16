package com.gitee.planners.api.pick

/**
 * 指向表现后端注册表。
 *
 * 服务端只允许存在一个表现实现：脚本在启动时注册，重载或停用前清空。
 * 未注册时首次指向会走 [PickFailureReason.NO_PRESENTATION] 失败反馈，而不是静默无响应。
 */
object PickPresentationRegistry {

    @Volatile
    private var presentation: PickPresentation? = null

    /**
     * 注册表现后端。
     *
     * @param value 表现实现。
     */
    fun register(value: PickPresentation) {
        presentation = value
    }

    /**
     * 取当前表现后端。
     *
     * @return 表现实现；未注册时返回 null。
     */
    fun getOrNull(): PickPresentation? {
        return presentation
    }

    /**
     * 清空表现后端，用于重载与停用。
     */
    fun clear() {
        presentation = null
    }
}
