package com.gitee.planners.core.ui

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.Registries
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.skill.formatter.DynamicSkillIcon
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
    private val levelOffsets = ConcurrentHashMap<Player, Int>()

    @Option("__option__.icon-learned")
    val iconLearned = simpleConfigNodeTo<ConfigurationSection, SkillIconAppend> { SkillIconAppend(this) }

    @Option("__option__.icon-learn")
    val iconLearnCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    @Option("__option__.icon-upgrade")
    val iconUpgradeCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    @Option("__option__.icon-locked")
    val iconLockedCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    @Option("__option__.icon-beyond")
    val iconBeyondCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    @Option("__option__.icon-scroll-up")
    val iconScrollUpCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    @Option("__option__.icon-scroll-up-top")
    val iconScrollUpTopCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    @Option("__option__.icon-scroll-down")
    val iconScrollDownCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    @Option("__option__.icon-scroll-down-bottom")
    val iconScrollDownBottomCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    @Option("__option__.icon-scroll-left")
    val iconScrollLeftCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    @Option("__option__.icon-scroll-left-end")
    val iconScrollLeftEndCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    @Option("__option__.icon-scroll-right")
    val iconScrollRightCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    @Option("__option__.icon-scroll-right-end")
    val iconScrollRightEndCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    @Option("__option__.icon-level-label")
    val iconLevelLabelCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

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
        val maxLv = ordered.mapNotNull { tree.immutable.nodes[it]?.maxLevel }.maxOrNull() ?: 1
        val maxLevelOffset = maxOf(0, (maxLv - 1) / 6)
        val levelOffset = levelOffsets.getOrDefault(player, 0).coerceIn(0, maxLevelOffset)
        createUI(player, route, tree, ordered, scroll, levelOffset, maxLevelOffset).openTo(player)
    }

    override fun display(player: Player): BaseUI.Display {
        throw UnsupportedOperationException("Use open(player) instead")
    }

    private fun createUI(
        player: Player,
        route: PlayerRoute,
        tree: PlayerRoute.SkillTree,
        ordered: List<String>,
        scroll: Int,
        levelOffset: Int,
        maxLevelOffset: Int
    ): BaseUI {
        val total = ordered.size
        val startLv = levelOffset * 6 + 1
        return BaseUI.createBaseUI {
            BaseUI.chest(this@PlayerSkillTreeUI) {

                onBuild { _, inv ->
                    setDecorateIcon(decorateIcon.get(), inv)
                }

                // Row 0: up/down (技能翻页) + level labels
                set(0, buildScrollUpItem(scroll)) {
                    if (scroll > 0) {
                        scrollOffsets[player] = scroll - 1
                        open(player)
                    }
                }
                for (i in 0 until 6) {
                    val lv = startLv + i
                    val cfg = iconLevelLabelCfg.get()
                    val label = cfg.icon.clone()
                    val meta = label.itemMeta
                    if (meta != null && meta.hasDisplayName()) {
                        meta.setDisplayName(meta.displayName.replace("{lv}", lv.toString()).colored())
                        label.itemMeta = meta
                    }
                    set(2 + i, label) {}
                }
                set(8, buildScrollDownItem(scroll, total)) {
                    if (scroll + 4 < total) {
                        scrollOffsets[player] = scroll + 1
                        open(player)
                    }
                }

                // Rows 1-4: Skill rows
                for (row in 0 until 4) {
                    val skillIdx = scroll + row
                    if (skillIdx < total) {
                        buildSkillRow(player, tree, ordered[skillIdx], row, startLv)
                    }
                }

                // Row 5: up(45) | SP(46) | left(47) | right(52) | down(53)
                set(45, buildScrollUpItem(scroll)) {
                    if (scroll > 0) {
                        scrollOffsets[player] = scroll - 1
                        open(player)
                    }
                }
                buildSPDisplay(route, 46)
                set(47, buildScrollLeftItem(levelOffset)) {
                    if (levelOffset > 0) {
                        levelOffsets[player] = levelOffset - 1
                        open(player)
                    }
                }
                set(52, buildScrollRightItem(levelOffset, maxLevelOffset)) {
                    if (levelOffset < maxLevelOffset) {
                        levelOffsets[player] = levelOffset + 1
                        open(player)
                    }
                }
                set(53, buildScrollDownItem(scroll, total)) {
                    if (scroll + 4 < total) {
                        scrollOffsets[player] = scroll + 1
                        open(player)
                    }
                }
            }
        }
    }

    // === Skill row builder ===

    private fun BaseUI.Chest.buildSkillRow(
        player: Player,
        tree: PlayerRoute.SkillTree,
        skillId: String,
        row: Int,
        startLv: Int
    ) {
        val baseSlot = (row + 1) * 9
        val skillNode = tree.immutable.nodes[skillId] ?: return
        val level = tree.getLevel(skillId)
        val immutableSkill = Registries.SKILL.getOrNull(skillId) ?: return

        // Col0: skill icon (current level)
        set(baseSlot, DynamicSkillIcon.build(player, immutableSkill, level)) {}

        // Col2-7: level slots (startLv to startLv+5)
        for (i in 0 until 6) {
            val slotLv = startLv + i
            val colSlot = baseSlot + 2 + i
            if (slotLv > skillNode.maxLevel) {
                set(colSlot, buildIconFromConfig(iconBeyondCfg.get(), mapOf("{lv}" to slotLv.toString()))) {}
            } else {
                val item = buildLevelItem(player, tree, immutableSkill, skillId, slotLv, level)
                if (slotLv == level + 1) {
                    set(colSlot, item) {
                        handleLevelClick(player, tree, skillId, slotLv, level)
                    }
                } else {
                    set(colSlot, item) {}
                }
            }
        }
    }

    // === Level slot items ===

    private fun buildLevelItem(
        player: Player,
        tree: PlayerRoute.SkillTree,
        skill: com.gitee.planners.core.config.ImmutableSkill,
        skillId: String,
        slotLv: Int,
        currentLevel: Int
    ): ItemStack {
        val placeholders = mapOf("{lv}" to slotLv.toString(), "{skillId}" to skill.id)
        if (slotLv <= currentLevel) {
            val item = DynamicSkillIcon.build(player, skill, slotLv)
            setAppend(item, iconLearned.get(), placeholders)
            return item
        }
        if (slotLv == currentLevel + 1) {
            val cfg = if (currentLevel == 0) iconLearnCfg.get() else iconUpgradeCfg.get()
            return buildIconFromConfig(cfg, placeholders, getHints(tree, player, skillId))
        }
        val item = buildIconFromConfig(iconLockedCfg.get(), placeholders, getHints(tree, player, skillId))
        return item
    }

    private fun getHints(tree: PlayerRoute.SkillTree, player: Player, skillId: String): List<String> {
        val result = if (tree.getLevel(skillId) > 0) {
            tree.canUpgrade(player, skillId)
        } else {
            tree.canLearn(player, skillId)
        }
        if (result.passed) {
            return emptyList()
        }
        return result.hints
    }

    // === Icon builders ===

    private fun buildIconFromConfig(cfg: Icon, placeholders: Map<String, String>, extraLore: List<String> = emptyList()): ItemStack {
        val item = cfg.icon.clone()
        val meta = item.itemMeta ?: return item
        if (meta.hasDisplayName()) {
            meta.setDisplayName(meta.displayName.replacePlaceholders(placeholders).colored())
        }
        val lore = meta.lore?.colored()?.map { it.replacePlaceholders(placeholders) } ?: mutableListOf()
        val coloredExtra = extraLore.map { "&c$it".colored() }
        meta.lore = lore + coloredExtra
        item.itemMeta = meta
        return item
    }

    private fun setAppend(item: ItemStack, cfg: SkillIconAppend, placeholders: Map<String, String>) {
        val meta = item.itemMeta ?: return
        val lore = meta.lore?.colored() ?: mutableListOf()
        val originalName = if (meta.hasDisplayName()) meta.displayName.colored() else ""
        val nameAppend = cfg.nameAppend.replacePlaceholders(placeholders).colored()
        val loreAppend = cfg.loreAppend.map { it.replacePlaceholders(placeholders).colored() }
        meta.setDisplayName(originalName + nameAppend)
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

    private fun buildScrollLeftItem(levelOffset: Int): ItemStack {
        val cfg = if (levelOffset > 0) iconScrollLeftCfg.get() else iconScrollLeftEndCfg.get()
        return cfg.icon.clone().applyColored()
    }

    private fun buildScrollRightItem(levelOffset: Int, maxLevelOffset: Int): ItemStack {
        val cfg = if (levelOffset < maxLevelOffset) iconScrollRightCfg.get() else iconScrollRightEndCfg.get()
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

    private fun BaseUI.Chest.buildSPDisplay(route: PlayerRoute, slot: Int) {
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
        set(slot, icon) {}
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
