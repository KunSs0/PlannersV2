package com.gitee.planners.core.config

import com.gitee.planners.api.common.script.ComplexCompiledScript
import com.gitee.planners.api.common.script.ComplexScriptPlatform
import com.gitee.planners.api.common.script.SingletonKetherScript
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.Skill
import com.gitee.planners.api.job.Variable
import com.gitee.planners.util.getOption
import com.gitee.planners.util.mapValueWithId
import taboolib.common.LifeCycle
import taboolib.common.platform.function.postpone
import taboolib.library.xseries.getItemStack
import taboolib.module.configuration.Configuration

class ImmutableSkill(config: Configuration) : Skill, ComplexCompiledScript {

    override val id = config.file!!.nameWithoutExtension

    private val option = config.getOption()

    val icon = option.getItemStack("icon-formatter")

    override val async = option.getBoolean("async",true)

    val action = config.getString("action", config.getString("run", "tell none"))!!

    init {
        // 在 ENABLE 阶段 编译脚本
        postpone(LifeCycle.ENABLE) { this.compiledScript() }
    }

    val startedLevel = option.getInt("started-level", 0)

    val immutableVariables = option.mapValueWithId("variables") { id: String, value: Any ->
        ImmutableVariable.parse(id, value)
    }

    override fun source(): String {
        return action
    }

    override fun namespaces(): List<String> {
        return listOf(KetherHelper.NAMESPACE_COMMON, KetherHelper.NAMESPACE_SKILL)
    }

    override fun platform(): ComplexScriptPlatform {
        return ComplexScriptPlatform.SKILL
    }
    override fun getVariableOrNull(id: String): Variable? {
        return immutableVariables[id]
    }

    override fun getVariables(): Map<String, Variable> {
        return immutableVariables
    }

    override fun toString(): String {
        return "ImmutableSkill(id='$id', action='$action', startedLevel=$startedLevel, immutableVariables=$immutableVariables)"
    }


}
