package com.gitee.planners.core.skill.formatter

import com.gitee.planners.api.PlannersAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.asTarget
import com.gitee.planners.module.script.ScriptContext
import com.gitee.planners.module.script.ScriptManager
import com.gitee.planners.module.script.ScriptOptions
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skilltree.SkillTreeNodeEffectService
import com.gitee.scriptengine.api.ScriptSession
import org.bukkit.entity.Player
import taboolib.module.chat.colored
import java.util.LinkedHashMap
import java.util.regex.Matcher


class DynamicSkillIcon(sender: ProxyTarget<*>, skill: ImmutableSkill, level: Int = 1) :
    AbstractSkillIcon(sender, skill, level) {

    private val profiling = RenderProfiling()

    private val options by lazy {
        createOptions(null)
    }

    private fun createOptions(sessionPool: RenderSessionPool?): ScriptOptions {
        val optionStart = System.nanoTime()
        val player = sender.instance as? Player
        val result: ScriptOptions
        if (player != null) {
            result = PlannersAPI.newOptions(player, skill, level)
        } else {
            result = ScriptOptions.forSkill(sender.instance ?: sender, level, skill)
        }
        profiling.optionsCreateMs += elapsedMs(optionStart)
        val contextSetupStart = System.nanoTime()
        if (sessionPool == null) {
            evaluateVariables(result)
        } else {
            sessionPool.evaluate(result, skill.immutableVariables, profiling)
        }
        return result
    }

    private fun evaluateVariables(result: ScriptOptions) {
        val contextSetupStart = System.nanoTime()
        val previousContext = ScriptContext.getCurrent()
        ScriptContext.setCurrent(result.variables)
        profiling.scriptContextMs += elapsedMs(contextSetupStart)
        val sessionOpenStart = System.nanoTime()
        val session = ScriptManager.openSession(result)
        profiling.scriptSessionOpenMs += elapsedMs(sessionOpenStart)
        try {
            for ((id, variable) in skill.immutableVariables) {
                val variableStart = System.nanoTime()
                val value = variable.evaluate(session)
                profiling.variableEvalMs += elapsedMs(variableStart)
                profiling.variableEvalCount += 1
                result.set(id, value)
            }
        } finally {
            val sessionCloseStart = System.nanoTime()
            session.close()
            profiling.scriptSessionCloseMs += elapsedMs(sessionCloseStart)
            val contextRestoreStart = System.nanoTime()
            if (previousContext == null) {
                ScriptContext.clear()
            } else {
                ScriptContext.setCurrent(previousContext)
            }
            profiling.scriptContextMs += elapsedMs(contextRestoreStart)
        }
    }

    override fun parse(text: String?): String {
        if (text == null) {
            return ""
        }
        // 在模板替换开始前初始化变量，避免将懒加载的脚本计算重复计入模板耗时。
        val preparedOptions = options
        val templateStart = System.nanoTime()
        val matcher = ImmutableSkill.displayTemplatePattern.matcher(text.trim())
        val rendered = StringBuffer()
        while (matcher.find()) {
            val key = matcher.group(1)
            val value = preparedOptions.variables[key]
            check(value != null) {
                "技能 '${skill.id}' 的图标占位符 {{$key}} 未计算出变量值"
            }
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value.toString()))
            profiling.templateResolveCount += 1
        }
        matcher.appendTail(rendered)
        profiling.templateResolveMs += elapsedMs(templateStart)
        return rendered.toString()
    }

    /**
     * 只渲染技能图标的文本字段，不创建或复制 ItemStack。
     */
    fun renderText(): RenderedIcon {
        return renderText(options)
    }

    /**
     * 使用当前快照的共享会话池渲染图标文本。
     *
     * @param sessionPool 当前快照持有的会话池。
     * @return 已解析的图标文本与阶段统计。
     */
    fun renderText(sessionPool: RenderSessionPool): RenderedIcon {
        val renderingOptions = createOptions(sessionPool)
        return renderText(renderingOptions)
    }

    private fun renderText(renderingOptions: ScriptOptions): RenderedIcon {
        val name = skill.displayIconName
        val renderedName: String?
        if (name == null) {
            renderedName = null
        } else {
            val parsedName = renderTemplate(name, renderingOptions)
            val colorStart = System.nanoTime()
            renderedName = parsedName.colored()
            profiling.colorizeMs += elapsedMs(colorStart)
            profiling.colorizeCount += 1
        }
        val renderedLore = ArrayList<String>()
        for (line in skill.displayIconLore) {
            val parsedLine = renderTemplate(line, renderingOptions)
            val colorStart = System.nanoTime()
            renderedLore.add(parsedLine.colored())
            profiling.colorizeMs += elapsedMs(colorStart)
            profiling.colorizeCount += 1
        }
        return RenderedIcon(renderedName, renderedLore, profiling)
    }

    private fun renderTemplate(text: String, renderingOptions: ScriptOptions): String {
        val templateStart = System.nanoTime()
        val matcher = ImmutableSkill.displayTemplatePattern.matcher(text.trim())
        val rendered = StringBuffer()
        while (matcher.find()) {
            val key = matcher.group(1)
            val value = renderingOptions.variables[key]
            check(value != null) {
                "技能 '${skill.id}' 的图标占位符 {{$key}} 未计算出变量值"
            }
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value.toString()))
            profiling.templateResolveCount += 1
        }
        matcher.appendTail(rendered)
        profiling.templateResolveMs += elapsedMs(templateStart)
        return rendered.toString()
    }

    /**
     * 已完成的图标文本与本次渲染的阶段统计。
     *
     * @property name 已解析的图标名称。
     * @property lore 已解析的图标 Lore。
     * @property profiling 本次图标渲染阶段耗时。
     */
    class RenderedIcon(val name: String?, val lore: List<String>, val profiling: RenderProfiling)

    /**
     * 单次职业快照内共享的动态图标脚本会话池。
     *
     * 会话按技能 Prelude 列表分组，生命周期严格限制在单次快照内，不能跨玩家或跨请求复用。
     */
    class RenderSessionPool : AutoCloseable {

        private val sessions = LinkedHashMap<List<String>, ScriptSession>()

        /**
         * 使用匹配的 Prelude 会话计算一个技能的全部图标变量。
         *
         * @param options 当前技能运行时选项。
         * @param variables 当前技能声明的变量。
         * @param profiling 当前图标的性能统计。
         */
        fun evaluate(options: ScriptOptions, variables: Map<String, com.gitee.planners.core.config.ImmutableVariable>, profiling: RenderProfiling) {
            val key = ArrayList(options.preludeScripts)
            var session = sessions[key]
            if (session == null) {
                val openStart = System.nanoTime()
                session = ScriptManager.openReusableSession(options, variables.keys)
                profiling.scriptSessionOpenMs += elapsedMs(openStart)
                sessions[key] = session
            } else {
                val rebindStart = System.nanoTime()
                ScriptManager.rebindReusableSession(session, options, variables.keys)
                profiling.scriptSessionRebindMs += elapsedMs(rebindStart)
            }
            val contextSetupStart = System.nanoTime()
            val previousContext = ScriptContext.getCurrent()
            ScriptContext.setCurrent(options.variables)
            profiling.scriptContextMs += elapsedMs(contextSetupStart)
            try {
                for ((id, variable) in variables) {
                    val variableStart = System.nanoTime()
                    val value = variable.evaluate(session)
                    profiling.variableEvalMs += elapsedMs(variableStart)
                    profiling.variableEvalCount += 1
                    options.set(id, value)
                }
            } finally {
                val contextRestoreStart = System.nanoTime()
                if (previousContext == null) {
                    ScriptContext.clear()
                } else {
                    ScriptContext.setCurrent(previousContext)
                }
                profiling.scriptContextMs += elapsedMs(contextRestoreStart)
            }
        }

        override fun close() {
            for (session in sessions.values) {
                session.close()
            }
            sessions.clear()
        }

        private fun elapsedMs(startNanos: Long): Double {
            return (System.nanoTime() - startNanos) / 1_000_000.0
        }
    }

    /**
     * 单次动态图标渲染的阶段耗时统计。
     */
    class RenderProfiling {

        var optionsCreateMs: Double = 0.0
        var scriptContextMs: Double = 0.0
        var scriptSessionOpenMs: Double = 0.0
        var scriptSessionCloseMs: Double = 0.0
        var scriptSessionRebindMs: Double = 0.0
        var variableEvalMs: Double = 0.0
        var variableEvalCount: Int = 0
        var templateResolveMs: Double = 0.0
        var templateResolveCount: Int = 0
        var colorizeMs: Double = 0.0
        var colorizeCount: Int = 0
    }

    /**
     * 将单调纳秒起点换算为当前已耗毫秒。
     *
     * @param startNanos 单调纳秒起点。
     * @return 已耗毫秒。
     */
    private fun elapsedMs(startNanos: Long): Double {
        return (System.nanoTime() - startNanos) / 1_000_000.0
    }

    companion object {

        fun build(player: Player, skill: PlayerSkill): org.bukkit.inventory.ItemStack {
            val level = SkillTreeNodeEffectService.getSkillLevel(player.plannersTemplate, skill.id)
            return build(player, skill.immutable, level)
        }

        fun build(player: Player, skill: ImmutableSkill, level: Int = 1) =
            DynamicSkillIcon(player.asTarget(), skill, level).build()

        fun render(player: Player, skill: ImmutableSkill, level: Int = 1): RenderedIcon {
            val formatter = DynamicSkillIcon(player.asTarget(), skill, level)
            return formatter.renderText()
        }

        /**
         * 创建一个仅供单次快照使用的动态图标会话池。
         *
         * @return 新建的会话池；调用方完成快照后必须关闭。
         */
        fun createRenderSessionPool(): RenderSessionPool {
            return RenderSessionPool()
        }

        /**
         * 使用快照共享会话池渲染动态图标文本。
         *
         * @param player 渲染目标玩家。
         * @param skill 技能定义。
         * @param level 当前技能等级。
         * @param sessionPool 当前快照的会话池。
         * @return 已解析的图标文本与阶段统计。
         */
        fun render(player: Player, skill: ImmutableSkill, level: Int, sessionPool: RenderSessionPool): RenderedIcon {
            val formatter = DynamicSkillIcon(player.asTarget(), skill, level)
            return formatter.renderText(sessionPool)
        }

    }


}
