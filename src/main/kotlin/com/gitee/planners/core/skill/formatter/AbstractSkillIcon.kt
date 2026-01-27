package com.gitee.planners.core.skill.formatter

import com.gitee.planners.api.job.target.Target
import com.gitee.planners.core.config.ImmutableSkill
import org.bukkit.inventory.ItemStack
import taboolib.module.chat.colored
import taboolib.platform.util.buildItem

abstract class AbstractSkillIcon(val sender: Target<*>, val skill: ImmutableSkill, val level: Int) : SkillIcon {

    override fun build(): ItemStack {
        return buildItem(skill.icon!!) {
            this.name = parse(name)
            val newList = parse(this.lore)
            this.lore.clear()
            this.lore += newList.colored()
        }
    }

    abstract fun parse(text: String?): String

    open fun parse(texts: List<String>): List<String> {
        return texts.map { parse(it) }
    }

}
