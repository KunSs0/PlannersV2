package com.gitee.planners.core.skill.formatter

import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.core.config.ImmutableSkill
import org.bukkit.inventory.ItemStack
import taboolib.module.chat.colored
import taboolib.platform.util.buildItem

/**
 * 技能图标渲染基类。
 *
 * 名称与 Lore 由子类按当前执行上下文渲染；展示文本里的 `<...>` 由求值表达式解析，
 * 纯文本配置不产生求值开销。
 */
abstract class AbstractSkillIcon(val sender: ProxyTarget<*>, val skill: ImmutableSkill, val level: Int) : SkillIcon {

    override fun build(): ItemStack {
        val base = skill.icon ?: error("技能 '${skill.id}' 缺少 display.icon 配置")
        return buildItem(base) {
            val renderedName = renderName()
            if (renderedName != null) {
                this.name = renderedName
            }
            this.lore.clear()
            this.lore += renderLore().colored()
        }
    }

    /**
     * 渲染图标名称。
     *
     * @return 渲染结果；未配置名称时返回 null。
     */
    abstract fun renderName(): String?

    /**
     * 渲染图标 Lore。
     *
     * @return 逐行渲染结果。
     */
    abstract fun renderLore(): List<String>
}
