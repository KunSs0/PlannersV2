package com.gitee.planners.core.skill.formatter

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.asTarget
import com.gitee.planners.core.skill.context.SkillExecutionContext
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skilltree.SkillTreeNodeEffectService
import org.bukkit.entity.Player
import taboolib.module.chat.colored
import java.util.LinkedHashMap
import java.util.regex.Matcher


class DynamicSkillIcon(sender: ProxyTarget<*>, skill: ImmutableSkill, level: Int = 1) :
    AbstractSkillIcon(sender, skill, level) {

    private val profiling = RenderProfiling()

    private val execution by lazy { createExecution() }

    private fun createExecution(): SkillExecutionContext {
        val optionStart = System.nanoTime()
        val result = SkillExecutionContext.create(sender, level, skill)
        profiling.optionsCreateMs += elapsedMs(optionStart)
        val variableStart = System.nanoTime()
        val values = skill.evaluateDisplayVariables(result)
        for ((id, value) in values) {
            result.setVariable(id, value)
        }
        profiling.variableEvalMs += elapsedMs(variableStart)
        profiling.variableEvalCount += values.size
        return result
    }

    override fun parse(text: String?): String {
        if (text == null) {
            return ""
        }
        // 在模板替换开始前初始化变量，避免将懒加载的脚本计算重复计入模板耗时。
        val preparedExecution = execution
        val templateStart = System.nanoTime()
        val matcher = ImmutableSkill.displayTemplatePattern.matcher(text.trim())
        val rendered = StringBuffer()
        while (matcher.find()) {
            val expression = matcher.group(1)
            // 模板业务表达式始终通过启动期预编译的 Nova SourceUnit 执行。
            val value = skill.evaluateDisplayTemplate(expression, preparedExecution)
            if (value == null) {
                throw IllegalStateException("Skill '${skill.id}' display template returned null: {{$expression}}")
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
        return renderText(execution)
    }

    private fun renderText(renderingExecution: SkillExecutionContext): RenderedIcon {
        val name = skill.displayIconName
        val renderedName: String?
        if (name == null) {
            renderedName = null
        } else {
            val parsedName = renderTemplate(name, renderingExecution)
            val colorStart = System.nanoTime()
            renderedName = parsedName.colored()
            profiling.colorizeMs += elapsedMs(colorStart)
            profiling.colorizeCount += 1
        }
        val renderedLore = ArrayList<String>()
        for (line in skill.displayIconLore) {
            val parsedLine = renderTemplate(line, renderingExecution)
            val colorStart = System.nanoTime()
            renderedLore.add(parsedLine.colored())
            profiling.colorizeMs += elapsedMs(colorStart)
            profiling.colorizeCount += 1
        }
        return RenderedIcon(renderedName, renderedLore, profiling)
    }

    private fun renderTemplate(text: String, renderingExecution: SkillExecutionContext): String {
        val templateStart = System.nanoTime()
        val matcher = ImmutableSkill.displayTemplatePattern.matcher(text.trim())
        val rendered = StringBuffer()
        while (matcher.find()) {
            val expression = matcher.group(1)
            // 模板业务表达式始终通过启动期预编译的 Nova SourceUnit 执行。
            val value = skill.evaluateDisplayTemplate(expression, renderingExecution)
            if (value == null) {
                throw IllegalStateException("Skill '${skill.id}' display template returned null: {{$expression}}")
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

    }


}
