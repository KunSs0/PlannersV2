package com.gitee.planners.core.config

import com.gitee.planners.api.common.Unique
import com.gitee.planners.api.job.Variable
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.module.script.ScriptContext
import com.gitee.planners.module.script.ScriptManager
import com.gitee.planners.module.script.ScriptOptions
import com.gitee.planners.util.getOption
import com.gitee.planners.util.mapValueWithId
import taboolib.common.platform.function.warning
import taboolib.common.util.asList
import taboolib.library.configuration.ConfigurationSection
import taboolib.library.xseries.getItemStack
import taboolib.module.configuration.Configuration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class ImmutableSkill(config: Configuration) : Unique {

    /** 技能ID */
    override val id = config.file!!.nameWithoutExtension

    /** 技能名称 */
    val name: String = config.getString("__option__.name", id)!!

    private val option = config.getOption()

    /** 技能图标 */
    val icon = option.getConfigurationSection("display")?.getItemStack("icon")

    /** 技能分类 */
    val categories = option["category", "*"]!!.asList()

    /** 技能是否异步运行 */
    val async = option.getBoolean("async", true)

    /** 脚本源代码 */
    val action = config.getString("action", config.getString("run", ""))!!

    /**
     * 技能提供的属性。
     * key = 属性键（在 registry 中为逻辑属性，否则为物理直通）
     * value = JS 表达式字符串或数字
     */
    val attributes: Map<String, String>
        get() {
            val section = option.getConfigurationSection("hook.attributes")
            if (section == null) {
                return emptyMap()
            }
            return section.getValues(false).mapValues { it.value.toString() }
        }

    /** 开始等级 */
    val startedLevel = option.getInt("started-level", 0)

    /** 最高等级 */
    val maxLevel = option.getInt("max-level", 10)

    val immutableVariables = option.mapValueWithId("variables") { id: String, value: Any ->
        ImmutableVariable.parse(id, value)
    }

    /** 外部插件实现的 Hook 标记接口 */
    interface Hook

    /** Hook 解码器：从配置段解析出 Hook 实例 */
    fun interface Decoder {
        fun decode(section: ConfigurationSection): Hook
    }

    /** 懒加载 hooks，首次访问时遍历 YAML __option__.hook.* 并用注册的 Decoder 解码 */
    val hooks: Map<String, Hook> by lazy {
        val section = config.getConfigurationSection("__option__.hook")
        if (section == null) {
            emptyMap<String, Hook>()
        } else {
            val result = linkedMapOf<String, Hook>()
            for (ns in section.getKeys(false)) {
                val nsSection = section.getConfigurationSection(ns)
                if (nsSection == null) {
                    continue
                }
                val decoder = decoders[ns]
                if (decoder == null) {
                    continue
                }
                result[ns] = decoder.decode(nsSection)
            }
            result
        }
    }

    companion object {
        private val decoders = ConcurrentHashMap<String, Decoder>()

        fun registerHook(namespace: String, decoder: Decoder) {
            decoders[namespace] = decoder
        }
    }

    /**
     * 执行技能脚本
     */
    fun execute(
        sender: ProxyTarget<*>,
        level: Int = 0,
        variables: Map<String, Any?> = emptyMap()
    ): CompletableFuture<Any?> {
        if (action.isEmpty()) {
            return CompletableFuture.completedFuture(null)
        }

        val options = ScriptOptions.of()
        options.set("sender", sender)
        options.set("origin", (sender as? ProxyTarget.Location<*>)?.getBukkitLocation())
        options.set("level", level)
        options.set("skill", this@ImmutableSkill)
        for ((k, v) in variables) {
            options.set(k, v)
        }

        val task = {
            val vars = options.variables
            ScriptContext.setCurrent(vars)
            val session = ScriptManager.openSession(options)
            try {
                session.eval(action)
                if (session.hasFunction("main")) {
                    session.invoke("main")
                } else null
            } catch (e: Throwable) {
                warning("[Skill] 技能脚本执行异常: $id")
                warning("[Skill] ${e.javaClass.simpleName}: ${e.message}")
                e.printStackTrace()
                null
            } finally {
                session.close()
                ScriptContext.clear()
            }
        }

        return if (async) {
            CompletableFuture.supplyAsync(task)
        } else {
            CompletableFuture.completedFuture(task())
        }
    }

    fun getVariableOrNull(id: String): Variable? {
        return immutableVariables[id]
    }

    fun getVariables(): Map<String, Variable> {
        return immutableVariables
    }

    override fun toString(): String = "ImmutableSkill(id='$id', action='$action', startedLevel=$startedLevel, immutableVariables=$immutableVariables)"

}
