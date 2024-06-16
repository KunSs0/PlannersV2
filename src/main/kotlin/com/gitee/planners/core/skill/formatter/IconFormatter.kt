package com.gitee.planners.core.skill.formatter

import com.gitee.planners.api.common.Formatter
import org.bukkit.inventory.ItemStack

interface IconFormatter : Formatter {

    fun build(): ItemStack

}
