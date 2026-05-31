package com.gitee.planners.core.ui

import com.gitee.planners.api.BackpackAPI
import com.gitee.planners.api.KeyBindingAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.Registries
import com.gitee.planners.core.skill.binding.MinecraftInteraction
import org.bukkit.entity.Player
import taboolib.library.configuration.ConfigurationSection
import com.gitee.planners.core.ui.BaseUI.Companion.setIcon
import taboolib.module.chat.colored
import taboolib.platform.util.isRightClick

object BackpackUI : AutomationBaseUI("backpack.yml") {

    @Option("__option__.slots")
    val uiSlots = simpleConfigNodeTo<List<*>, List<Int>> { map { (it as Number).toInt() } }

    @Option("__option__.icon-empty-slot")
    val emptySlotCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    @Option("__option__.icon-prev-page")
    val prevPageCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    @Option("__option__.icon-next-page")
    val nextPageCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    @Option("__option__.icon-page-info")
    val pageInfoCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    @Option("__option__.icon-skill-list")
    val skillListCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    @Option("__option__.icon-equipped-skill")
    val equippedSkillAppend = simpleConfigNodeTo<ConfigurationSection, SkillIconAppend> { SkillIconAppend(this) }

    @Option("__option__.icon-#")
    val borderCfg = simpleConfigNodeTo<ConfigurationSection, Icon> { Icon(this) }

    override fun display(player: Player): BaseUI.Display {
        val template = player.plannersTemplate
        val pages = Registries.BACKPACK.pages
        val currentPageId = BackpackAPI.getCurrentPage(template)
        val page = pages[currentPageId]
        if (page == null) {
            return BaseUI.chest(this) {}
        }
        val pageIds = pages.keys.toList()
        val currentIndex = pageIds.indexOf(currentPageId)

        return BaseUI.chest(this) {
            setIcon(borderCfg.get()) {}

            page.slots.entries.forEachIndexed { index, (slotId, slotConfig) ->
                if (index < uiSlots.get().size) {
                    val invSlot = uiSlots.get()[index]
                    val skill = template.getEquippedSkillByBackpackSlot(currentPageId, slotId)
                    if (skill != null) {
                        renderEquippedSkill(player, template, skill, slotConfig.key, invSlot)
                    } else {
                        renderEmptySlot(player, template, slotConfig.key, invSlot, currentPageId, slotId)
                    }
                }
            }

            if (currentIndex > 0) {
                setIcon(prevPageCfg.get()) {
                    val prevPage = pageIds[currentIndex - 1]
                    BackpackAPI.setCurrentPage(template, prevPage)
                    MinecraftInteraction.updateInventory(template)
                    BackpackUI.openTo(player)
                }
            }

            if (currentIndex < pageIds.size - 1) {
                setIcon(nextPageCfg.get()) {
                    val nextPage = pageIds[currentIndex + 1]
                    BackpackAPI.setCurrentPage(template, nextPage)
                    MinecraftInteraction.updateInventory(template)
                    BackpackUI.openTo(player)
                }
            }

            val infoCfg = pageInfoCfg.get()
            val infoIcon = infoCfg.icon.clone()
            val infoMeta = infoIcon.itemMeta
            if (infoMeta != null && infoMeta.hasDisplayName()) {
                infoMeta.setDisplayName(
                    infoMeta.displayName
                        .replace("{pageCurrent}", "${currentIndex + 1}")
                        .replace("{pageTotal}", "${pageIds.size}")
                        .colored()
                )
                infoIcon.itemMeta = infoMeta
            }
            setIcon(infoCfg, infoIcon) {}

            setIcon(skillListCfg.get()) {
                BackpackSkillSelectUI.choice(player) { }
            }
        }
    }

    private fun BaseUI.Chest.renderEmptySlot(
        player: Player,
        template: com.gitee.planners.core.player.PlayerTemplate,
        keyId: String,
        invSlot: Int,
        currentPageId: String,
        slotId: String
    ) {
        val keybinding = Registries.KEYBINDING.getOrNull(keyId)
        val name = keybinding?.name
        if (name == null) {
            return
        }
        val icon = emptySlotCfg.get().icon.clone()
        val meta = icon.itemMeta
        if (meta != null) {
            if (meta.hasDisplayName()) {
                meta.setDisplayName(meta.displayName.replace("{keyName}", name).colored())
            }
            val lore = meta.lore?.colored()?.map { it.replace("{keyName}", name) } ?: mutableListOf()
            meta.lore = lore
            icon.itemMeta = meta
        }
        set(invSlot, icon) {
            BackpackSkillSelectUI.choice(player) { selectedSkill ->
                BackpackAPI.equipSkill(template, selectedSkill, currentPageId, slotId)
                MinecraftInteraction.updateInventory(template)
                BackpackUI.openTo(player)
            }
        }
    }

    private fun BaseUI.Chest.renderEquippedSkill(
        player: Player,
        template: com.gitee.planners.core.player.PlayerTemplate,
        skill: com.gitee.planners.core.player.PlayerSkill,
        keyId: String,
        invSlot: Int
    ) {
        val icon = KeyBindingAPI.createIconFormatter(player, skill).build()
        val append = equippedSkillAppend.get()
        val keybinding = Registries.KEYBINDING.getOrNull(keyId)
        if (append.loreAppend.isNotEmpty() && keybinding != null) {
            val keyName = keybinding.name
            val meta = icon.itemMeta
            if (meta != null) {
                val lore = meta.lore?.colored() ?: mutableListOf()
                val replaced = append.loreAppend.map { it.replace("{keyName}", keyName).colored() }
                meta.lore = lore + replaced
                icon.itemMeta = meta
            }
        }
        set(invSlot, icon) {
            if (clickEvent().isRightClick) {
                BackpackAPI.unequipSkill(template, skill)
                MinecraftInteraction.updateInventory(template)
                BackpackUI.openTo(player)
            }
        }
    }
}
