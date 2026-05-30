package com.gitee.planners.core.ui

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.Registries
import com.gitee.planners.core.player.PlayerRoute
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.platform.util.asLangText
import taboolib.platform.util.buildItem
import taboolib.platform.util.sendLang
import java.util.concurrent.ConcurrentHashMap

/**
 * Skill tree UI - 9x6 chest inventory.
 */
object PlayerSkillTreeUI : AutomationBaseUI("skilltree.yml") {

    private val scrollOffsets = ConcurrentHashMap<Player, Int>()

    fun open(player: Player) {
        val template = player.plannersTemplate
        val route = template.route ?: run {
            player.sendLang("skill-tree-no-route")
            return
        }
        val tree = route.skillTree ?: run {
            player.sendLang("skill-tree-no-tree")
            return
        }
        val ordered = topologicalOrder(tree.immutable.graph)
        val maxScroll = maxOf(0, ordered.size - 4)
        val scroll = scrollOffsets.getOrDefault(player, 0).coerceIn(0, maxScroll)
        createUI(player, route, tree, ordered, scroll).openTo(player)
    }

    override fun display(player: Player): BaseUI.Display {
        throw UnsupportedOperationException("Use open(player) instead")
    }

    private fun createUI(
        player: Player,
        route: PlayerRoute,
        tree: PlayerRoute.SkillTree,
        ordered: List<String>,
        scroll: Int
    ): BaseUI {
        return BaseUI.createBaseUI {
            BaseUI.chest(this@PlayerSkillTreeUI) {

                onBuild { _, inv ->
                    setDecorateIcon(decorateIcon.get(), inv)
                }

                // Row 0: Scroll up
                set(0, buildScrollUpItem(player, scroll)) {
                    if (scroll > 0) {
                        scrollOffsets[player] = scroll - 1
                        open(player)
                    }
                }

                // Rows 1-4: Skill rows
                for (row in 0 until 4) {
                    val skillIdx = scroll + row
                    if (skillIdx < ordered.size) {
                        buildSkillRow(player, tree, ordered[skillIdx], row)
                    }
                }

                // Row 5: Scroll down + SP
                set(45, buildScrollDownItem(player, scroll, ordered.size)) {
                    if (scroll + 4 < ordered.size) {
                        scrollOffsets[player] = scroll + 1
                        open(player)
                    }
                }
                buildSPDisplay(player, route)
            }
        }
    }

    // === Skill row builder ===

    private fun BaseUI.Chest.buildSkillRow(
        player: Player,
        tree: PlayerRoute.SkillTree,
        skillId: String,
        row: Int
    ) {
        val baseSlot = (row + 1) * 9
        val skillNode = tree.immutable.nodes[skillId] ?: return
        val level = tree.getLevel(skillId)
        val immutableSkill = Registries.SKILL.getOrNull(skillId)
        val skillDisplayName = immutableSkill?.name ?: skillId

        // Col0: skill icon
        set(baseSlot, immutableSkill?.icon ?: ItemStack(Material.STONE)) {
            // click placeholder
        }

        // Col1: skill name + current level
        set(baseSlot + 1, buildSkillNameItem(player, skillDisplayName, skillId, level, skillNode.maxLevel))

        // Col3-8: level slots Lv1-Lv6
        for (lv in 1..6) {
            val colSlot = baseSlot + 2 + lv
            if (lv > skillNode.maxLevel) {
                set(colSlot, buildBeyondItem(player, lv))
            } else {
                val item = buildLevelItem(player, skillId, lv, level)
                set(colSlot, item) {
                    handleLevelClick(player, tree, skillId, lv, level)
                }
            }
        }
    }

    private fun buildSkillNameItem(player: Player, skillName: String, skillId: String, level: Int, maxLevel: Int): ItemStack {
        return buildItem(Material.PAPER) {
            name = player.asLangText("skill-tree-item-name", skillName)
            lore.add(player.asLangText("skill-tree-id-lore", skillId))
            lore.add(player.asLangText("skill-tree-level-lore", level, maxLevel))
            lore.add("")
        }
    }

    // === Level slot items ===

    private fun buildLevelItem(player: Player, skillId: String, slotLv: Int, currentLevel: Int): ItemStack {
        if (slotLv <= currentLevel) {
            return buildItem(Material.GOLD_NUGGET) {
                name = player.asLangText("skill-tree-learned-name", slotLv)
                lore.add(player.asLangText("skill-tree-already-mastered"))
                lore.add(player.asLangText("skill-tree-skill-lore", skillId))
            }
        }
        if (slotLv == currentLevel + 1) {
            val action = if (currentLevel == 0) {
                player.asLangText("skill-tree-learn-action")
            } else {
                player.asLangText("skill-tree-upgrade-action")
            }
            return buildItem(Material.KNOWLEDGE_BOOK) {
                name = player.asLangText("skill-tree-actionable-name", slotLv, action)
                lore.add(player.asLangText("skill-tree-actionable-lore", action))
                lore.add(player.asLangText("skill-tree-skill-lore", skillId))
            }
        }
        return buildItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE) {
            name = player.asLangText("skill-tree-locked-name", slotLv)
            lore.add(player.asLangText("skill-tree-need-previous"))
            lore.add(player.asLangText("skill-tree-skill-lore", skillId))
        }
    }

    private fun buildBeyondItem(player: Player, lv: Int): ItemStack {
        return buildItem(Material.GRAY_STAINED_GLASS_PANE) {
            name = player.asLangText("skill-tree-beyond-name", lv)
            lore.add(player.asLangText("skill-tree-beyond-lore"))
        }
    }

    // === Click handler ===

    private fun handleLevelClick(
        player: Player,
        tree: PlayerRoute.SkillTree,
        skillId: String,
        slotLv: Int,
        currentLevel: Int
    ) {
        if (slotLv != currentLevel + 1) return

        try {
            val future = if (currentLevel == 0) {
                tree.learn(player, skillId)
            } else {
                tree.upgrade(player, skillId)
            }

            future.thenAccept {
                open(player)
            }.exceptionally { e ->
                val msg = e.cause?.message ?: e.message ?: "Unknown error"
                player.sendLang("skill-tree-failed", msg)
                null
            }
        } catch (e: Exception) {
            player.sendLang("skill-tree-failed", e.message ?: "Unknown error")
        }
    }

    // === Scroll buttons ===

    private fun buildScrollUpItem(player: Player, scroll: Int): ItemStack {
        return buildItem(if (scroll > 0) Material.SPECTRAL_ARROW else Material.GRAY_DYE) {
            name = if (scroll > 0) {
                player.asLangText("skill-tree-scroll-up")
            } else {
                player.asLangText("skill-tree-scroll-up-top")
            }
        }
    }

    private fun buildScrollDownItem(player: Player, scroll: Int, total: Int): ItemStack {
        return buildItem(if (scroll + 4 < total) Material.SPECTRAL_ARROW else Material.GRAY_DYE) {
            name = if (scroll + 4 < total) {
                player.asLangText("skill-tree-scroll-down")
            } else {
                player.asLangText("skill-tree-scroll-down-bottom")
            }
        }
    }

    // === SP display ===

    private fun BaseUI.Chest.buildSPDisplay(player: Player, route: PlayerRoute) {
        set(48, buildItem(Material.NETHER_STAR) {
            name = player.asLangText("skill-tree-points-title")
            lore.add(player.asLangText("skill-tree-points-available", route.skillPointsCurrent))
            lore.add(player.asLangText("skill-tree-points-spent", route.skillPointsUsed))
            lore.add("")
            lore.add(player.asLangText("skill-tree-points-earned"))
        })
    }

    // === Topological sort ===

    private fun topologicalOrder(graph: Map<String, List<String>>): List<String> {
        val inDegree = mutableMapOf<String, Int>()
        graph.keys.forEach { inDegree[it] = 0 }
        graph.forEach { (node, prereqs) -> inDegree[node] = prereqs.size }

        val queue = ArrayDeque<String>()
        inDegree.filter { it.value == 0 }.keys.forEach { queue.add(it) }

        val result = mutableListOf<String>()
        while (queue.isNotEmpty()) {
            val node = queue.removeAt(0)
            result.add(node)
            graph.forEach { (other, prereqs) ->
                if (node in prereqs) {
                    val newDegree = (inDegree[other] ?: 1) - 1
                    inDegree[other] = newDegree
                    if (newDegree == 0) queue.add(other)
                }
            }
        }
        return result
    }
}
