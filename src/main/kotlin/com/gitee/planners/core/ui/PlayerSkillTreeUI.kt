package com.gitee.planners.core.ui

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.Registries
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.skill.formatter.DynamicSkillIcon
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.configuration.ConfigurationSection
import com.gitee.planners.core.ui.BaseUI.Companion.setIcon
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

    @Option("__option__.rows-per-page")
    val rowsPerPage = 4

    @Option("__option__.level-count")
    val levelCount = 6

    @Option("__option__.skill-icon-offset")
    val skillIconOffset = 0

    @Option("__option__.level-start-offset")
    val levelStartOffset = 2

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
        val maxScroll = maxOf(0, ordered.size - rowsPerPage)
        val scroll = scrollOffsets.getOrDefault(player, 0).coerceIn(0, maxScroll)
        val maxLv = ordered.mapNotNull { tree.immutable.nodes[it]?.maxLevel }.maxOrNull() ?: 1
        val maxLevelOffset = maxOf(0, (maxLv - 1) / levelCount)
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
        val startLv = levelOffset * levelCount + 1

        return BaseUI.createBaseUI {
            BaseUI.chest(this@PlayerSkillTreeUI) {

                onBuild { _, inv ->
                    setDecorateIcon(decorateIcon.get(), inv)
                }

                // 上翻
                setIcon(iconScrollUpCfg.get(), buildScrollIcon(scroll > 0, iconScrollUpCfg, iconScrollUpTopCfg)) {
                    if (scroll > 0) {
                        scrollOffsets[player] = scroll - 1
                        open(player)
                    }
                }
                // 等级标签
                val labelSlots = iconLevelLabelCfg.get().slots
                val labelCfg = iconLevelLabelCfg.get()
                for (i in 0 until minOf(levelCount, labelSlots.size)) {
                    val lv = startLv + i
                    val label = buildIconFromConfig(labelCfg, mapOf("{lv}" to lv.toString()))
                    set(labelSlots[i], label) {}
                }

                // Skill rows
                for (row in 0 until rowsPerPage) {
                    val skillIdx = scroll + row
                    if (skillIdx < total) {
                        buildSkillRow(player, tree, ordered[skillIdx], row, startLv)
                    }
                }

                // 下翻
                setIcon(iconScrollDownCfg.get(), buildScrollIcon(scroll + rowsPerPage < total, iconScrollDownCfg, iconScrollDownBottomCfg)) {
                    if (scroll + rowsPerPage < total) {
                        scrollOffsets[player] = scroll + 1
                        open(player)
                    }
                }
                // SP
                buildSPDisplay(route, iconSpDisplayCfg.get())
                // 左翻
                setIcon(iconScrollLeftCfg.get(), buildScrollIcon(levelOffset > 0, iconScrollLeftCfg, iconScrollLeftEndCfg)) {
                    if (levelOffset > 0) {
                        levelOffsets[player] = levelOffset - 1
                        open(player)
                    }
                }
                // 右翻
                setIcon(iconScrollRightCfg.get(), buildScrollIcon(levelOffset < maxLevelOffset, iconScrollRightCfg, iconScrollRightEndCfg)) {
                    if (levelOffset < maxLevelOffset) {
                        levelOffsets[player] = levelOffset + 1
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

        // Skill icon
        set(baseSlot + skillIconOffset, DynamicSkillIcon.build(player, immutableSkill, level)) {}

        // Level slots
        for (i in 0 until levelCount) {
            val slotLv = startLv + i
            val colSlot = baseSlot + levelStartOffset + i
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

    private fun buildScrollIcon(active: Boolean, activeCfg: SimpleConfigNodeTransfer<ConfigurationSection, Icon>, inactiveCfg: SimpleConfigNodeTransfer<ConfigurationSection, Icon>): ItemStack {
        val cfg = if (active) activeCfg.get() else inactiveCfg.get()
        return buildIconFromConfig(cfg, emptyMap())
    }

    // === SP display ===

    private fun BaseUI.Chest.buildSPDisplay(route: PlayerRoute, cfg: Icon) {
        val icon = buildIconFromConfig(cfg, mapOf(
            "{spCurrent}" to route.skillPointsCurrent.toString(),
            "{spUsed}" to route.skillPointsUsed.toString()
        ))
        setIcon(cfg, icon) {}
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
