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
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.common5.cdouble
import taboolib.common5.cint
import taboolib.library.xseries.getItemStack
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.util.mapSection

class ImmutableSkill(config: Configuration) : Skill, ComplexCompiledScript {

    override val id = config.file!!.nameWithoutExtension

    override val name: String = config.getString("__option__.name", id)!!

    private val option = config.getOption()

    val icon = option.getItemStack("icon-formatter")

    val category = option.getString("category", "*")!!

    override val async = option.getBoolean("async", true)

    val action = config.getString("action", config.getString("run", "tell none"))!!

    /** 升级条件 */
    val conditionAsUpgrade = option.mapSection("upgrade.condition") {
        val split = it.name.split("-")
        val begin = split[0].toInt()
        val end = split.getOrElse(1) { "$begin" }.cint
        IndexedUpgrade(begin, end, it.mapValueWithId { _, value -> value.cdouble })
    }

    fun getConditionAsUpgrade(index: Int): IndexedUpgrade? {
        conditionAsUpgrade.values.forEach {
            if (it.begin == it.end && it.begin == index) {
                return it
            } else if (index in it.begin..it.end) {
                return it
            }
        }

        return null
    }

    init {
        // 在 ENABLE 阶段 编译脚本
        registerLifeCycleTask(LifeCycle.ENABLE) { this.compiledScript() }
    }

    /** 开始等级 */
    val startedLevel = option.getInt("started-level", 0)

    /** 最高等级 */
    val maxLevel = option.getInt("max-level", 10)

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

    class IndexedUpgrade(val begin: Int, val end: Int, args: Map<String, Double>) : HashMap<String, Double>(args)


}
