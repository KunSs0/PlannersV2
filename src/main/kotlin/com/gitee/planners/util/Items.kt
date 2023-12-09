package com.gitee.planners.util

import org.bukkit.inventory.ItemStack
import taboolib.platform.util.buildItem
import taboolib.platform.util.modifyLore

/**
 * 替换插入
 */
fun ItemStack.replaceInfix(node: String, elements: () -> List<String>): ItemStack {
    return buildItem(this) {
        val newList = mutableListOf<String>()
        this.lore.forEach {
            if (it.contains(node)) {
                newList += elements().map { it.replace(node, it) }
            } else {
                newList += it
            }
        }
        this.lore.clear()
        this.lore.addAll(newList)
    }
}
