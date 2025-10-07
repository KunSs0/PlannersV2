package com.gitee.planners.core.config

import com.gitee.planners.api.common.script.ComplexCompiledScript

/**
 * 状态定义
 */
interface State {

    val id: String

    val priority: Double

    /**
     * 是否为静态状态
     *
     * 静态状态不会被自动移除
     */
    val isStatic: Boolean

    val name: String

    val triggers: Map<String, Trigger>

    class Trigger(val id: String, val on: String, val action: ComplexCompiledScript)
}

