package com.gitee.planners.core.skill.formatter

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.asTarget
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.context.SkillExecutionContext
import com.gitee.planners.core.skilltree.SkillTreeNodeEffectService
import org.bukkit.entity.Player
import taboolib.module.chat.colored

/**
 * 技能动态图标。
 *
 * 名称与 Lore 统一交给技能的展示文本渲染：文本里的 `<...>` 按求值表达式解析。
 * 一次渲染只构造一个查询上下文，名称与逐行 Lore 共用它，变量只计算一次。
 */
class DynamicSkillIcon(sender: ProxyTarget<*>, skill: ImmutableSkill, level: Int = 1) :
    AbstractSkillIcon(sender, skill, level) {

    private val profiling = RenderProfiling()

    private val execution by lazy { SkillExecutionContext.create(sender, level, skill) }

    private val displayScope by lazy { skill.displayScope(execution) }

    override fun renderName(): String? {
        return skill.renderDisplayIconName(displayScope)
    }

    override fun renderLore(): List<String> {
        return skill.renderDisplayIconLore(displayScope)
    }

    /**
     * 只渲染技能图标的文本字段，不创建或复制 ItemStack。
     *
     * 技能名、图标名与逐行 Lore 共用同一个查询上下文。
     *
     * @return 已上色的名称与 Lore，附本次渲染统计。
     */
    fun renderText(): RenderedIcon {
        val scope = displayScope
        val skillName = skill.renderName(scope)
        val sourceName = skill.renderDisplayIconName(scope)
        val renderedName: String?
        if (sourceName == null) {
            renderedName = null
        } else {
            val colorStart = System.nanoTime()
            renderedName = sourceName.colored()
            profiling.colorizeMs += elapsedMs(colorStart)
            profiling.colorizeCount += 1
        }
        val sourceLore = skill.renderDisplayIconLore(scope)
        val renderedLore = ArrayList<String>(sourceLore.size)
        for (line in sourceLore) {
            val colorStart = System.nanoTime()
            renderedLore.add(line.colored())
            profiling.colorizeMs += elapsedMs(colorStart)
            profiling.colorizeCount += 1
        }
        return RenderedIcon(skillName, renderedName, renderedLore, profiling)
    }

    /**
     * 已完成的图标文本与本次渲染的阶段统计。
     *
     * @property skillName 已插值的技能名称。
     * @property name 已解析的图标名称。
     * @property lore 已解析的图标 Lore。
     * @property profiling 本次图标渲染阶段耗时。
     */
    class RenderedIcon(
        val skillName: String,
        val name: String?,
        val lore: List<String>,
        val profiling: RenderProfiling,
    )

    /**
     * 单次动态图标渲染的阶段耗时统计。
     */
    class RenderProfiling {

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

        /**
         * 按玩家当前的技能等级构建图标物品。
         *
         * @param player 玩家。
         * @param skill 玩家在线技能。
         * @return 图标物品。
         */
        fun build(player: Player, skill: PlayerSkill): org.bukkit.inventory.ItemStack {
            val level = SkillTreeNodeEffectService.getSkillLevel(player.plannersTemplate, skill.id)
            return build(player, skill.immutable, level)
        }

        /**
         * 构建图标物品。
         *
         * @param player 玩家。
         * @param skill 技能定义。
         * @param level 技能等级。
         * @return 图标物品。
         */
        fun build(player: Player, skill: ImmutableSkill, level: Int = 1): org.bukkit.inventory.ItemStack {
            return DynamicSkillIcon(player.asTarget(), skill, level).build()
        }

        /**
         * 只渲染文本，不创建 ItemStack。
         *
         * @param player 玩家。
         * @param skill 技能定义。
         * @param level 技能等级。
         * @return 已上色的名称与 Lore。
         */
        fun render(player: Player, skill: ImmutableSkill, level: Int = 1): RenderedIcon {
            val formatter = DynamicSkillIcon(player.asTarget(), skill, level)
            return formatter.renderText()
        }
    }
}
