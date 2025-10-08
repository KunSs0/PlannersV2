package com.gitee.planners.core.skill.entity.state

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.core.config.State
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor

class TargetStateHolder(val state: State, val duration: Long, val cb: Runnable?): Metadata.Unsaved {

    val end = System.currentTimeMillis() + duration * 50

    // 状态是否有效
    var isValid = true

    // 状态是否过期
    val isExpired: Boolean
        get() = !isValid && System.currentTimeMillis() >= end


    // 定时任务
    var task: PlatformExecutor.PlatformTask? = null

    /**
     * 初始化状态持有者
     */
    fun init() {
        task = submit(delay = duration, async = true) {
            isValid = false
            cb?.run()
        }
    }

    /**
     * 关闭状态持有者
     */
    fun close() {
        if (task != null) {
            task!!.cancel()
        }

        this.isValid = false
    }

    companion object {

        /**
         * 创建状态持有者
         *
         * @param state 状态
         * @param duration 持续时间
         */
        fun create(state: State, duration: Long, cb: Runnable?): TargetStateHolder {
            val end = System.currentTimeMillis() + duration * 50

            return TargetStateHolder(state, end, cb)
        }

        /**
         * 解析元数据
         *
         * @param metadata 元数据
         */
        fun parse(metadata: Metadata?): TargetStateHolder? {
            if (metadata == null) {
                return null
            }

            return metadata.any() as TargetStateHolder
        }

    }
}