package com.gitee.planners.core.config

import com.gitee.planners.module.action.ActionSpec

/**
 * 状态定义接口。
 */
interface State {

    companion object {

        const val METADATA_PATH = "__pl.state"

        /** 状态加载时执行一次的流程名。 */
        const val FLOW_LOAD = "onLoad"

        /** 状态附加时触发的流程名。 */
        const val FLOW_ATTACH = "onAttach"

        /** 状态移除时触发的流程名。 */
        const val FLOW_DETACH = "onDetach"

        /** 状态首次挂载时触发的流程名。 */
        const val FLOW_MOUNT = "onMount"

        /** 状态完全关闭时触发的流程名。 */
        const val FLOW_CLOSE = "onClose"

        /** 状态自然结束时触发的流程名。 */
        const val FLOW_END = "onEnd"

        /** 允许出现在状态定义里的全部流程名。 */
        val FLOW_NAMES: Set<String> = setOf(FLOW_LOAD, FLOW_ATTACH, FLOW_DETACH, FLOW_MOUNT, FLOW_CLOSE, FLOW_END)

        fun State.path() = "__pl.state.${this.id}"
    }

    val id: String

    val priority: Double

    /**
     * 允许叠加的最大层数。
     */
    val maxLayer: Int

    val name: String

    /**
     * 状态提供的属性源。
     *
     * 每一层状态都会重复追加一次该列表，不参与 Planners 逻辑属性转换。
     */
    val attribute: List<String>

    /**
     * 状态生命周期流程。
     *
     * 流程名与事件一一对应：`onLoad`、`onAttach`、`onDetach`、`onMount`、
     * `onClose`、`onEnd`。
     */
    val flows: ActionSpec?
}
