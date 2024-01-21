package com.gitee.planners.core.config

import com.gitee.planners.api.common.script.ComplexCompiledScript
import com.gitee.planners.api.common.script.ComplexScriptPlatform
import com.gitee.planners.api.common.script.SingletonKetherScript
import com.gitee.planners.api.common.script.kether.KetherHelper
import com.gitee.planners.api.job.Skill
import com.gitee.planners.api.job.Variable
import com.gitee.planners.util.getOption
import com.gitee.planners.util.mapValueWithId
import taboolib.module.configuration.Configuration

class ImmutableSkill(config: Configuration) : Skill, ComplexCompiledScript {

    override val id = config.file!!.nameWithoutExtension

    val action = config.getString("action", config.getString("run", "tell none"))!!

    init {
        this.compiledScript()

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

    private val option = config.getOption()

    val startedLevel = option.getInt("started-level", 0)


    val immutableVariables = option.mapValueWithId("variables") { id: String, value: Any ->
        ImmutableVariable.parse(id, value)
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
