package com.gitee.planners.core.ui

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.Registries
import com.gitee.planners.core.player.PlayerRoute
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.platform.util.buildItem
import java.util.concurrent.ConcurrentHashMap

/**
 * Skill tree UI - 9x6 chest inventory.
 */
object PlayerSkillTreeUI : AutomationBaseUI("skilltree.yml") {

    private val scrollOffsets = ConcurrentHashMap<Player, Int>()

    fun open(player: Player) {
        val template = player.plannersTemplate
        val route = template.route ?: run {
            player.sendMessage("§cYou haven't selected a job route yet")
            return
        }
        val tree = route.skillTree ?: run {
            player.sendMessage("§cCurrent route has no skill tree")
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
                set(0, buildScrollUpItem(scroll)) {
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
                set(45, buildScrollDownItem(scroll, ordered.size)) {
                    if (scroll + 4 < ordered.size) {
                        scrollOffsets[player] = scroll + 1
                        open(player)
                    }
                }
                buildSPDisplay(route)
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
        set(baseSlot + 1, buildSkillNameItem(skillDisplayName, skillId, level, skillNode.maxLevel))

        // Col3-8: level slots Lv1-Lv6
        for (lv in 1..6) {
            val colSlot = baseSlot + 2 + lv
            if (lv > skillNode.maxLevel) {
                set(colSlot, buildBeyondItem(lv))
            } else {
                val item = buildLevelItem(skillId, lv, level)
                set(colSlot, item) {
                    handleLevelClick(player, tree, skillId, lv, level)
                }
            }
        }
    }

    private fun buildSkillNameItem(skillName: String, skillId: String, level: Int, maxLevel: Int): ItemStack {
        return buildItem(Material.PAPER) {
            name = "§e$skillName"
            lore.add("§7ID: §f$skillId")
            lore.add("§7Level: §fLv$level / Lv$maxLevel")
            lore.add("")
        }
    }

    // === Level slot items ===

    private fun buildLevelItem(skillId: String, slotLv: Int, currentLevel: Int): ItemStack {
        if (slotLv <= currentLevel) {
            return buildItem(Material.GOLD_NUGGET) {
                name = "§eLv$slotLv §a* Learned"
                lore.add("§7Already mastered")
                lore.add("§7Skill: §f$skillId")
            }
        }
        if (slotLv == currentLevel + 1) {
            val action = if (currentLevel == 0) "Learn" else "Upgrade"
            return buildItem(Material.KNOWLEDGE_BOOK) {
                name = "§eLv$slotLv §6§l>> $action"
                lore.add("§e§lClick to $action")
                lore.add("§7Skill: §f$skillId")
            }
        }
        return buildItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE) {
            name = "§eLv$slotLv §7Locked"
            lore.add("§7Need previous level first")
            lore.add("§7Skill: §f$skillId")
        }
    }

    private fun buildBeyondItem(lv: Int): ItemStack {
        return buildItem(Material.GRAY_STAINED_GLASS_PANE) {
            name = "§8- Lv$lv"
            lore.add("§7Beyond max level")
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
                player.sendMessage("§cFailed: $msg")
                null
            }
        } catch (e: Exception) {
            player.sendMessage("§c${e.message}")
        }
    }

    // === Scroll buttons ===

    private fun buildScrollUpItem(scroll: Int): ItemStack {
        return buildItem(if (scroll > 0) Material.SPECTRAL_ARROW else Material.GRAY_DYE) {
            name = if (scroll > 0) "§e▲ Scroll Up" else "§7▲ At Top"
        }
    }

    private fun buildScrollDownItem(scroll: Int, total: Int): ItemStack {
        return buildItem(if (scroll + 4 < total) Material.SPECTRAL_ARROW else Material.GRAY_DYE) {
            name = if (scroll + 4 < total) "§e▼ Scroll Down" else "§7▼ At Bottom"
        }
    }

    // === SP display ===

    private fun BaseUI.Chest.buildSPDisplay(route: PlayerRoute) {
        set(48, buildItem(Material.NETHER_STAR) {
            name = "§e✦ Skill Points"
            lore.add("§fAvailable: §6${route.skillPointsCurrent}")
            lore.add("§fTotal spent: §7${route.skillPointsUsed}")
            lore.add("")
            lore.add("§7Earned by leveling up")
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
