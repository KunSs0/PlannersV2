package com.gitee.planners.core.config

import com.gitee.planners.module.action.ActionParser
import com.gitee.planners.module.action.ActionSpec
import taboolib.library.configuration.ConfigurationSection

/**
 * 状态定义。
 *
 * 生命周期用命名流程声明；历史的一整段脚本写法已随脚本运行时移除，
 * 残留配置在加载期直接报错，避免静默失效。
 *
 * @property config 状态配置节。
 */
class ImmutableState(val config: ConfigurationSection) : State {

    override val id: String = config.name

    override val priority: Double = config.getDouble("priority")

    override val maxLayer: Int
    get() {
        val value = config.getInt("max-layer")
        if (value > 0) {
            return value
        }
        return Int.MAX_VALUE
    }

    override val name: String = config.getString("name", id)!!

    override val attribute: List<String> = config.getStringList("attribute")

    override val flows: ActionSpec?
    init {
        val raw = config.getString("action")
        if (raw != null && raw.isNotBlank()) {
            throw IllegalArgumentException(
                "state:$id.action 仍是脚本字符串写法；请改成 onLoad/onAttach 等命名流程。",
            )
        }
        val parsed = ActionParser.parseNamedFlows(config, "state:$id", State.FLOW_NAMES)
        flows = if (parsed.isEmpty()) {
            null
        } else {
            ActionSpec("state:$id", parsed)
        }
    }
}
