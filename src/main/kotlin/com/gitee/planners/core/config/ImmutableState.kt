package com.gitee.planners.core.config

import com.gitee.planners.module.action.ActionParser
import com.gitee.planners.module.action.ActionSpec
import com.gitee.planners.module.script.NovaScriptUnit
import com.gitee.planners.module.script.ScriptManager
import taboolib.library.configuration.ConfigurationSection

/**
 * 状态定义。
 *
 * 生命周期既可以用命名流程声明（推荐），也可以保留旧的一整段脚本；
 * 两者同时存在时以流程为准，避免重复执行。
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

    override val action: String?
    get() {
        val raw = config.getString("action")
        if (raw != null && raw.isNotBlank()) {
            return raw
        }
        return null
    }

    override val flows: ActionSpec?
    init {
        val parsed = ActionParser.parseNamedFlows(config, "state:$id", State.FLOW_NAMES)
        flows = if (parsed.isEmpty()) {
            null
        } else {
            ActionSpec("state:$id", parsed)
        }
    }

    override val actionModule: NovaScriptUnit?
    init {
        val source = action
        if (source == null || flows != null) {
            actionModule = null
        } else {
            actionModule = ScriptManager.compileModule("state:$id:action", source)
        }
    }
}
