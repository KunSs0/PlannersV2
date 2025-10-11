package com.gitee.planners.api.job.target

import com.gitee.planners.core.config.State

interface CapableState {

    /**
     * 目标对象当前是否仍然有效。
     */
    fun isValid(): Boolean

    /**
     * 为目标挂载状态。
     *
     * @param state 状态定义
     * @param duration 持续时间（tick），非静态状态必须为正数
     * @param refreshDuration 若状态已存在时是否刷新剩余时间
     */
    fun attachState(state: State, duration: Long = -1, refreshDuration: Boolean)

    /**
     * 按层数卸载状态。
     *
     * @param state 状态定义
     * @param layer 要移除的层数，传入 999 表示直接清空
     */
    fun detachState(state: State, layer: Int = 1)

    /**
     * 完整移除状态。
     *
     * @param state 状态定义
     */
    fun removeState(state: State)

    /**
     * 检查目标是否拥有指定状态。
     */
    fun hasState(state: State): Boolean

    /**
     * 检查目标上的状态是否已经过期。
     */
    fun isExpired(state: State): Boolean
}
