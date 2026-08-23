package com.gitee.planners.core.ui

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.core.config.ImmutableSkillTree
import com.gitee.planners.core.config.SkillTreeAttributeNode
import com.gitee.planners.core.config.SkillTreeNode
import com.gitee.planners.core.config.SkillTreeSkillNode
import com.gitee.planners.core.player.PlayerRoute
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.platform.util.sendLang

object PlayerSkillTreeUI : AutomationBaseUI("skilltree.yml") {

    fun open(player: Player) {
        val router = player.plannersTemplate.playerRouter
        if (router == null) {
            player.sendLang("skill-tree-no-route")
            return
        }
        val route = router.currentRoute
        val entries = createEntries(route)
        if (entries.isEmpty()) {
            player.sendLang("skill-tree-no-tree")
            return
        }
        createUI(player, route, entries).openTo(player)
    }

    override fun display(player: Player): BaseUI.Display {
        throw UnsupportedOperationException("Use open(player) instead")
    }

    private fun createEntries(route: PlayerRoute): List<NodeEntry> {
        val result = ArrayList<NodeEntry>()
        for (tree in route.skillTrees) {
            for (node in tree.nodes.values) {
                result.add(NodeEntry(tree, node))
            }
        }
        return result
    }

    private fun createUI(player: Player, route: PlayerRoute, entries: List<NodeEntry>): BaseUI {
        return BaseUI.createBaseUI {
            BaseUI.chest(this@PlayerSkillTreeUI) {
                onBuild { _, inventory ->
                    setDecorateIcon(decorateIcon.get(), inventory)
                }
                val slotLimit = minOf(entries.size, 54)
                for (index in 0 until slotLimit) {
                    val entry = entries[index]
                    val item = createNodeItem(route, entry)
                    set(index, item) {
                        advanceNode(player, route, entry)
                    }
                }
            }
        }
    }

    private fun createNodeItem(route: PlayerRoute, entry: NodeEntry): ItemStack {
        val node = entry.node
        val material = if (node is SkillTreeSkillNode) {
            Material.DIAMOND_SWORD
        } else {
            Material.NETHER_STAR
        }
        val item = ItemStack(material)
        val meta = item.itemMeta
        if (meta == null) {
            return item
        }
        val level = route.getNodeLevel(entry.tree.id, node.id)
        meta.setDisplayName("${getNodeName(node)} Lv$level/${node.maxLevel}")
        val lore = ArrayList<String>()
        lore.add("树: ${entry.tree.name}")
        lore.add("节点: ${node.id}")
        if (node is SkillTreeAttributeNode) {
            lore.add("属性节点")
        }
        meta.lore = lore
        item.itemMeta = meta
        return item
    }

    private fun getNodeName(node: SkillTreeNode): String {
        if (node is SkillTreeSkillNode) {
            return node.skillId
        }
        return node.id
    }

    private fun advanceNode(player: Player, route: PlayerRoute, entry: NodeEntry) {
        try {
            route.advanceNode(player, entry.tree.id, entry.node.id).thenAccept {
                open(player)
            }.exceptionally { exception ->
                val message = exception.message ?: "Unknown error"
                player.sendLang("skill-tree-failed", message)
                null
            }
        } catch (exception: Exception) {
            val message = exception.message ?: "Unknown error"
            player.sendLang("skill-tree-failed", message)
        }
    }

    private class NodeEntry(
        val tree: ImmutableSkillTree,
        val node: SkillTreeNode
    )
}
