package com.gitee.planners.module.fluxon

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.asTarget
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.skill.context.SkillContext
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.tabooproject.fluxon.parser.ParsedScript
import org.tabooproject.fluxon.runtime.Environment
import taboolib.common5.cdouble
import taboolib.common5.cint
import java.util.concurrent.CompletableFuture
import java.util.function.Function

/**
 * Fluxon 脚本接口
 * 替代原 KetherScript
 */
interface FluxonScript {

    /**
     * 执行脚本
     */
    fun run(options: FluxonScriptOptions): CompletableFuture<Any?>

    companion object {
        val PARSER_INT = Function<Any?, Int> { it.cint }
        val PARSER_DOUBLE = Function<Any?, Double> { it.cdouble }
        val PARSER_STRING = Function<Any?, String> { it.toString() }
        val PARSER_BOOLEAN = Function<Any?, Boolean> { it.toString().toBoolean() }
        val PARSER_ANY = Function<Any?, Any?> { it }
        val PARSER_VOID = Function<Any?, Unit> { }

        inline fun <reified T> FluxonScript.getNow(options: FluxonScriptOptions, parser: Function<Any?, T>): T {
            return parser.apply(run(options).getNow(null))
        }
    }

}

/**
 * Fluxon 脚本选项
 * 替代原 KetherScriptOptions
 */
class FluxonScriptOptions {

    internal val variables = mutableMapOf<String, Any?>()
    internal var async: Boolean = false

    fun set(key: String, value: Any?): FluxonScriptOptions {
        variables[key] = value
        return this
    }

    fun async(async: Boolean): FluxonScriptOptions {
        this.async = async
        return this
    }

    fun getVariables(): Map<String, Any?> = variables

    fun isAsync(): Boolean = async

    /**
     * 应用到 Environment
     */
    fun applyTo(env: Environment) {
        variables.forEach { (k, v) -> env.defineRootVariable(k, v) }
    }

    companion object {
        fun new(): FluxonScriptOptions = FluxonScriptOptions()

        /**
         * 设置发送者
         */
        fun sender(sender: Any, options: FluxonScriptOptions): FluxonScriptOptions {
            return FluxonScriptOptions().also { new ->
                new.variables.putAll(options.variables)
                new.async = options.async
                new.set("sender", sender)
            }
        }

        /**
         * 通用选项
         */
        fun common(sender: Any): FluxonScriptOptions {
            return FluxonScriptOptions().also {
                it.set("sender", sender)
                if (sender is Player) {
                    it.set("profile", sender.plannersTemplate)
                }
            }
        }

        /**
         * 创建选项
         */
        fun create(builder: FluxonScriptOptions.() -> Unit): FluxonScriptOptions {
            return FluxonScriptOptions().also(builder)
        }

        /**
         * 创建技能执行选项
         */
        fun forSkill(
            sender: Any,
            level: Int,
            skill: ImmutableSkill? = null,
            extraVars: Map<String, Any?> = emptyMap()
        ): FluxonScriptOptions {
            val proxyTarget = when (sender) {
                is ProxyTarget<*> -> sender
                is Entity -> sender.asTarget()
                else -> null
            }
            val player = when (sender) {
                is Player -> sender
                is ProxyTarget<*> -> sender.instance as? Player
                else -> null
            }
            return FluxonScriptOptions().also {
                it.set("sender", sender)
                it.set("level", level)
                it.set("ctx", SkillContext(proxyTarget, skill, level))
                if (player != null) {
                    it.set("profile", player.plannersTemplate)
                }
                extraVars.forEach { (k, v) -> it.set(k, v) }
            }
        }
    }
}

/**
 * 单例 Fluxon 脚本
 * 替代原 SingletonKetherScript
 */
open class SingletonFluxonScript(source: String? = null) : FluxonScript {

    open val action: String = source ?: ""

    val isNotNull: Boolean
        get() = action.isNotEmpty()

    /** 解析后的脚本 (延迟加载) */
    private val script: ParsedScript? by lazy {
        if (action.isEmpty()) null
        else FluxonScriptCache.getOrParse(action)
    }

    override fun run(options: FluxonScriptOptions): CompletableFuture<Any?> {
        val parsedScript = script ?: return CompletableFuture.completedFuture(null)

        val env = parsedScript.newEnvironment()
        options.applyTo(env)

        return if (options.isAsync()) {
            CompletableFuture.supplyAsync { parsedScript.eval(env) }
        } else {
            CompletableFuture.completedFuture(parsedScript.eval(env))
        }
    }

    /**
     * 同步执行并返回结果
     */
    fun eval(options: FluxonScriptOptions = FluxonScriptOptions()): Any? {
        val parsedScript = script ?: return null
        val env = parsedScript.newEnvironment()
        options.applyTo(env)
        return parsedScript.eval(env)
    }

    companion object {
        /**
         * 替换嵌套脚本表达式
         * 替代原 KetherFunction.reader.replaceNested
         */
        fun replaceNested(text: String, options: FluxonScriptOptions): String {
            val pattern = Regex("\\{\\{(.+?)}}")
            return pattern.replace(text) { match ->
                val expr = match.groupValues[1]
                SingletonFluxonScript(expr).eval(options)?.toString() ?: ""
            }
        }
    }
}
