package com.gitee.planners.core.ui

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.Registries
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.skill.formatter.DynamicSkillIcon
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.chat.colored
import taboolib.platform.util.sendLang
import java.util.concurrent.ConcurrentHashMap

/**
 * Skill tree UI - 9x6 chest inventory.
 */
object PlayerSkillTreeUI : AutomationBaseUI("skilltree.yml") {

    private val scrollOffsets = ConcurrentHashMap<Player, Int>()

    @Option("__option__.icon-learned")
    val iconLearned = simpleConfigNodeTo<ConfigurationSection, SkillIconAppend> { SkillIconAppend(this) }

    @Option("__option__.icon-learn")
    val iconLearn = simpleConfigNodeTo<ConfigurationSection, SkillIconAppend> { SkillIconAppend(this) }

    @Option("__option__.icon-upgrade")
    val iconUpgrade = simpleConfigNodeTo<ConfigurationSection, SkillIconAppend> { SkillIconAppend(this) }

    @Option("__option__.icon-locked")
    val iconLocked = simpleConfigNodeTo<ConfigurationSection, SkillIconAppend> { SkillIconAppend(this) }

    @Option("__option__.icon-beyond")
    val iconBeyondCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    @Option("__option__.icon-skill-info")
    val iconSkillInfo = simpleConfigNodeTo<ConfigurationSection, SkillIconAppend> { SkillIconAppend(this) }

    @Option("__option__.icon-scroll-up")
    val iconScrollUpCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    @Option("__option__.icon-scroll-up-top")
    val iconScrollUpTopCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    @Option("__option__.icon-scroll-down")
    val iconScrollDownCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    @Option("__option__.icon-scroll-down-bottom")
    val iconScrollDownBottomCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    @Option("__option__.icon-sp-display")
    val iconSpDisplayCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

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
        val immutableSkill = Registries.SKILL.getOrNull(skillId) ?: return

        // Col0: skill icon (current level)
        set(baseSlot, DynamicSkillIcon.build(player, immutableSkill, level)) {}

        // Col1: skill name + current level
        set(baseSlot + 1, buildSkillNameItem(player, immutableSkill, level, skillNode.maxLevel)) {}

        // Col3-8: level slots Lv1-Lv6
        for (lv in 1..6) {
            val colSlot = baseSlot + 2 + lv
            if (lv > skillNode.maxLevel) {
                set(colSlot, buildBeyondItem(lv)) {}
            } else {
                val item = buildLevelItem(player, immutableSkill, lv, level)
                if (lv == level + 1) {
                    set(colSlot, item) {
                        handleLevelClick(player, tree, skillId, lv, level)
                    }
                } else {
                    set(colSlot, item) {}
                }
            }
        }
    }

    private fun buildSkillNameItem(player: Player, skill: com.gitee.planners.core.config.ImmutableSkill, level: Int, maxLevel: Int): ItemStack {
        val item = DynamicSkillIcon.build(player, skill, level)
        val cfg = iconSkillInfo.get()
        setAppend(item, cfg, mapOf(
            "{skillName}" to skill.name,
            "{skillId}" to skill.id,
            "{level}" to level.toString(),
            "{maxLevel}" to maxLevel.toString()
        ))
        return item
    }

    // === Level slot items ===

    private fun buildLevelItem(player: Player, skill: com.gitee.planners.core.config.ImmutableSkill, slotLv: Int, currentLevel: Int): ItemStack {
        val item = DynamicSkillIcon.build(player, skill, slotLv)
        if (slotLv <= currentLevel) {
            setAppend(item, iconLearned.get(), mapOf("{lv}" to slotLv.toString(), "{skillId}" to skill.id))
        } else if (slotLv == currentLevel + 1) {
            if (currentLevel == 0) {
                setAppend(item, iconLearn.get(), mapOf("{lv}" to slotLv.toString(), "{skillId}" to skill.id))
            } else {
                setAppend(item, iconUpgrade.get(), mapOf("{lv}" to slotLv.toString(), "{skillId}" to skill.id))
            }
        } else {
            setAppend(item, iconLocked.get(), mapOf("{lv}" to slotLv.toString(), "{skillId}" to skill.id))
        }
        return item
    }

    private fun buildBeyondItem(lv: Int): ItemStack {
        val cfg = iconBeyondCfg.get()
        val item = cfg.icon.clone()
        val meta = item.itemMeta ?: return item
        meta.setDisplayName(meta.displayName.replace("{lv}", lv.toString()).colored())
        if (meta.hasLore()) {
            meta.lore = meta.lore!!.colored()
        }
        item.itemMeta = meta
        return item
    }

    private fun setAppend(item: ItemStack, cfg: SkillIconAppend, placeholders: Map<String, String>) {
        val meta = item.itemMeta ?: return
        val lore = meta.lore?.colored() ?: mutableListOf()
        val nameAppend = cfg.nameAppend.replacePlaceholders(placeholders).colored()
        val loreAppend = cfg.loreAppend.map { it.replacePlaceholders(placeholders).colored() }
        meta.setDisplayName(nameAppend)
        meta.lore = lore + loreAppend
        item.itemMeta = meta
    }

    private fun String.replacePlaceholders(placeholders: Map<String, String>): String {
        var result = this
        for ((key, value) in placeholders) {
            result = result.replace(key, value)
        }
        return result
    }

    private fun List<String>.replacePlaceholders(placeholders: Map<String, String>): List<String> {
        return this.map { it.replacePlaceholders(placeholders) }
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

    private fun buildScrollUpItem(scroll: Int): ItemStack {
        val cfg = if (scroll > 0) iconScrollUpCfg.get() else iconScrollUpTopCfg.get()
        return cfg.icon.clone().applyColored()
    }

    private fun buildScrollDownItem(scroll: Int, total: Int): ItemStack {
        val cfg = if (scroll + 4 < total) iconScrollDownCfg.get() else iconScrollDownBottomCfg.get()
        return cfg.icon.clone().applyColored()
    }

    private fun ItemStack.applyColored(): ItemStack {
        val meta = itemMeta ?: return this
        if (meta.hasDisplayName()) {
            meta.setDisplayName(meta.displayName.colored())
        }
        if (meta.hasLore()) {
            meta.lore = meta.lore!!.colored()
        }
        itemMeta = meta
        return this
    }

    // === SP display ===

    private fun BaseUI.Chest.buildSPDisplay(route: PlayerRoute) {
        val cfg = iconSpDisplayCfg.get()
        val icon = cfg.icon.clone()
        val meta = icon.itemMeta ?: return
        meta.setDisplayName(meta.displayName.colored())
        if (meta.hasLore()) {
            val lore = meta.lore!!.map {
                it.replace("{spCurrent}", route.skillPointsCurrent.toString())
                    .replace("{spUsed}", route.skillPointsUsed.toString())
                    .colored()
            }
            meta.lore = lore
        }
        icon.itemMeta = meta
        set(48, icon) {}
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
