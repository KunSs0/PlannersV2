package com.gitee.planners.api.job.target

import com.gitee.planners.core.config.State
import com.gitee.planners.core.skill.entity.state.EntityStateManager

/**
 * 检查目标是否拥有指定状态
 */
fun ProxyTarget.Entity<*>.hasState(state: State): Boolean {
    return EntityStateManager.has(this, state)
}

/**
 * 检查目标上的状态是否已经过期
 */
fun ProxyTarget.Entity<*>.isExpired(state: State): Boolean {
    return EntityStateManager.isExpired(this, state)
}

/**
 * 为目标挂载状态
 *
 * @param state 状态定义
 * @param duration 持续时间（tick），非静态状态必须为正数
 * @param refreshDuration 若状态已存在时是否刷新剩余时间
 */
fun ProxyTarget.Entity<*>.attachState(state: State, duration: Long, refreshDuration: Boolean = false) {
    EntityStateManager.attach(this, state, duration, refreshDuration)
}

/**
 * 按层数卸载状态
 *
 * @param state 状态定义
 * @param layer 要移除的层数，传入 999 表示直接清空
 */
fun ProxyTarget.Entity<*>.detachState(state: State, layer: Int = 1) {
    EntityStateManager.detach(this, state, layer)
}

/**
 * 完整移除状态
 *
 * @param state 状态定义
 */
fun ProxyTarget.Entity<*>.removeState(state: State) {
    EntityStateManager.remove(this, state)
}
