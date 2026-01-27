package com.gitee.planners.core.config

import com.gitee.planners.api.job.Skill
import com.gitee.planners.api.job.Variable
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.TargetLocation
import com.gitee.planners.module.fluxon.FluxonScriptCache
import com.gitee.planners.util.getOption
import com.gitee.planners.util.mapValueWithId
import org.tabooproject.fluxon.parser.ParsedScript
import taboolib.common.LifeCycle
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.common.platform.function.warning
import taboolib.common.util.asList
import taboolib.common5.cint
import taboolib.library.configuration.ConfigurationSection
import taboolib.library.xseries.getItemStack
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.util.mapSection
import java.util.concurrent.CompletableFuture

class ImmutableSkill(config: Configuration) : Skill {

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
    val async = option.getBoolean("async", true)

    /** 脚本源代码 */
    val action = config.getString("action", config.getString("run", ""))!!

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

    /** 解析后的脚本 (延迟加载) */
    val script: ParsedScript? by lazy {
        if (action.isEmpty()) {
            null
        } else {
            try {
                FluxonScriptCache.getOrParse(action)
            } catch (e: Exception) {
                warning("[Skill] 脚本解析失败: $id - ${e.message}")
                null
            }
        }
    }

    init {
        // 在 ENABLE 阶段预编译脚本
        registerLifeCycleTask(LifeCycle.ENABLE) {
            // 触发延迟加载
            script
        }
    }

    /** 开始等级 */
    val startedLevel = option.getInt("started-level", 0)

    /** 最高等级 */
    val maxLevel = option.getInt("max-level", 10)

    val immutableVariables = option.mapValueWithId("variables") { id: String, value: Any ->
        ImmutableVariable.parse(id, value)
    }

    /**
     * 执行技能脚本
     */
    fun execute(
        sender: Target<*>,
        level: Int = 0,
        variables: Map<String, Any?> = emptyMap()
    ): CompletableFuture<Any?> {
        val parsedScript = script ?: return CompletableFuture.completedFuture(null)

        val env = parsedScript.newEnvironment().apply {
            defineRootVariable("sender", sender)
            defineRootVariable("origin", (sender as? TargetLocation<*>)?.getBukkitLocation())
            defineRootVariable("level", level)
            defineRootVariable("skill", this@ImmutableSkill)
            variables.forEach { (k, v) -> defineRootVariable(k, v) }
        }

        return if (async) {
            CompletableFuture.supplyAsync { parsedScript.eval(env) }
        } else {
            CompletableFuture.completedFuture(parsedScript.eval(env))
        }
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

        class Amount(val expression: String, val mark: Boolean) {

            /** 解析后的脚本 */
            private val script: ParsedScript? by lazy {
                if (expression.isEmpty()) null
                else FluxonScriptCache.getOrParse(expression)
            }

            val isNotNull: Boolean
                get() = expression.isNotEmpty()

            /**
             * 执行表达式
             */
            fun eval(variables: Map<String, Any?> = emptyMap()): Any? {
                val parsedScript = script ?: return null
                val env = parsedScript.newEnvironment().apply {
                    variables.forEach { (k, v) -> defineRootVariable(k, v) }
                }
                return parsedScript.eval(env)
            }
        }
    }
}
