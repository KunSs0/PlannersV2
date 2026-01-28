package com.gitee.planners.core.config

import org.tabooproject.fluxon.parser.ParsedScript

/**
 * 状态定义接口。
 */
interface State {

    companion object {

        const val METADATA_PATH = "__pl.state"

        fun State.path() = "__pl.state.${this.id}"
    }

    val id: String

    val priority: Double

    /**
     * 允许叠加的最大层数。
     */
    val maxLayer: Int

    /**
     * 是否为静态状态，静态状态不会被系统自动移除。
     */
    val isStatic: Boolean

    val name: String

    /**
     * 状态脚本，通过约定函数名调用内置事件处理器。
     *
     * 内置事件函数：
     * - main(): 状态加载时执行一次
     * - onStateAttach(): 状态附加时触发
     * - onStateDetach(): 状态移除时触发
     * - onStateMount(): 状态首次挂载时触发
     * - onStateClose(): 状态完全关闭时触发
     * - onStateEnd(): 状态自然结束时触发
     */
    val action: ParsedScript?
}
