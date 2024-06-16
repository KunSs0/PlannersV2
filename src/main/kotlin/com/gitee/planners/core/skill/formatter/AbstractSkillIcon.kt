package com.gitee.planners.core.skill.formatter

import com.gitee.planners.api.job.target.Target
import com.gitee.planners.core.config.ImmutableSkill
import org.bukkit.inventory.ItemStack
import taboolib.module.kether.runKether
import taboolib.platform.util.buildItem

abstract class AbstractSkillIcon(val sender: Target<*>, val skill: ImmutableSkill, val level: Int) : SkillIcon {

    override fun build(): ItemStack {

        return buildItem(skill.icon!!) {
            runKether {
                this.name = parse(name)
                this.lore.clear()
                this.lore += parse(this.lore)
            }
        }

    }

    abstract fun parse(text: String?): String

    open fun parse(texts: List<String>): List<String> {
        return texts.map { parse(it) }
    }

}
