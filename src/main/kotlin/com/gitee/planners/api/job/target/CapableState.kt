package com.gitee.planners.api.job.target

import com.gitee.planners.core.config.State

interface CapableState {

    /**
     * 是否有效
     *
     * @return 是否有效
     */
    fun isValid():Boolean

    /**
     * 添加状态
     *
     * @param state 状态
     * @param duration 持续时间
     * @param coverBefore 是否覆盖之前的状态
     */
    fun addState(state: State, duration: Long = -1, coverBefore: Boolean)

    /**
     * 移除状态
     *
     * @param state 状态
     */
    fun removeState(state: State)

    /**
     * 是否有状态
     *
     * @param state 状态
     * @return 是否有状态
     */
    fun hasState(state: State): Boolean

    /**
     * 状态是否过期
     *
     * @param state 状态
     * @return 状态是否过期
     */
    fun isExpired(state: State): Boolean


}