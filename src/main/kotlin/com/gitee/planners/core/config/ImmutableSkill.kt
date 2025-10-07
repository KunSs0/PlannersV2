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
import taboolib.common.platform.function.info
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.common.util.asList
import taboolib.common5.cint
import taboolib.library.configuration.ConfigurationSection
import taboolib.library.xseries.getItemStack
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.util.mapSection
import taboolib.module.kether.printKetherErrorMessage
import taboolib.module.kether.runKether

class ImmutableSkill(config: Configuration) : Skill, ComplexCompiledScript {

    /** 技能ID */
    override val id = config.file!!.nameWithoutExtension

    /** 技能名称 */
    override val name: String = config.getString("__option__.name", id)!!

    private val option = config.getOption()

    /** 技能图标 */
    val icon = option.getItemStack("icon-formatter")

    /** 技能分类 */
    val categories = option["category", "*"]!!.asList()

    /** 技能是否异步运行 */
    override val async = option.getBoolean("async", true)

    val action = config.getString("action", config.getString("run", "tell none"))!!

    /**
     * 技能提供的属性
     */
    val attributes: List<String> = option.getStringList("hook.attributes")

    /** 升级条件 */
    val conditionAsUpgrade = option.mapSection("upgrade.condition") {
        val split = it.name.split("-")
        val begin = split[0].toInt()
        val end = split.getOrElse(1) { "$begin" }.cint
        IndexedUpgrade(begin, end, it.mapValueWithId { _, value ->
            if (value is ConfigurationSection) {
                IndexedUpgrade.Amount(value.getString("experience", "0")!!, value.getBoolean("mark", false))
            } else {
                IndexedUpgrade.Amount(value.toString(), false)
            }
        })
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
        registerLifeCycleTask(LifeCycle.ENABLE) {
            try {
                this.compiledScript()
            } catch (e: Exception) {
                e.printKetherErrorMessage(true)
            }
        }
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

    class IndexedUpgrade(val begin: Int, val end: Int, val args: Map<String, Amount>) {

        class Amount(experience: String, val mark: Boolean) : SingletonKetherScript(experience)

    }


}
