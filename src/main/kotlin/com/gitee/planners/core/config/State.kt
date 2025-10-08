package com.gitee.planners.core.config

import com.gitee.planners.api.common.script.ComplexCompiledScript
import taboolib.library.configuration.ConfigurationSection

/**
 * 状态定义
 */
interface State {

    companion object {

        const val METADATA_PATH = "__pl.state"


        fun State.path() = "__pl.state.${this.id}"

    }

    val id: String

    val priority: Double

    /**
     * 是否为静态状态
     * 静态状态不会被自动移除
     */
    val isStatic: Boolean

    val name: String

    val triggers: Map<String, Trigger>

    class Trigger(val id: String, val listen: String, val action: ComplexCompiledScript)
}

