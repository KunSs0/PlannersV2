package com.gitee.planners.core.ui

import com.gitee.planners.api.BackpackAPI
import com.gitee.planners.api.KeyBindingAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.Registries
import com.gitee.planners.core.skill.binding.MinecraftInteraction
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.configuration.ConfigurationSection
import taboolib.library.xseries.XItemStack
import taboolib.module.chat.colored
import taboolib.platform.util.isRightClick

object BackpackUI : AutomationBaseUI("backpack.yml") {

    @Option("__option__.slots")
    val uiSlots = simpleConfigNodeTo<List<*>, List<Int>> { map { (it as Number).toInt() } }

    @Option("__option__.icon-empty-slot")
    val emptySlotIcon = simpleConfigNodeTo<ConfigurationSection, ItemStack> { XItemStack.deserialize(this) }

    @Option("__option__.icon-prev-page")
    val prevPageIcon = simpleConfigNodeTo<ConfigurationSection, ItemStack> { XItemStack.deserialize(this) }

    @Option("__option__.icon-next-page")
    val nextPageIcon = simpleConfigNodeTo<ConfigurationSection, ItemStack> { XItemStack.deserialize(this) }

    @Option("__option__.icon-page-info")
    val pageInfoIcon = simpleConfigNodeTo<ConfigurationSection, ItemStack> { XItemStack.deserialize(this) }

    @Option("__option__.icon-skill-list")
    val skillListIcon = simpleConfigNodeTo<ConfigurationSection, ItemStack> { XItemStack.deserialize(this) }

    @Option("__option__.icon-equipped-skill")
    val equippedSkillAppend = simpleConfigNodeTo<ConfigurationSection, SkillIconAppend> { SkillIconAppend(this) }

    override fun display(player: Player): BaseUI.Display {
        val template = player.plannersTemplate
        val pages = Registries.BACKPACK.pages
        val currentPageId = BackpackAPI.getCurrentPage(template)
        val page = pages[currentPageId] ?: return BaseUI.chest(this) {}
        val pageIds = pages.keys.toList()
        val currentIndex = pageIds.indexOf(currentPageId)

        return BaseUI.chest(this) {
            // 槽位技能图标
            page.slots.entries.forEachIndexed { index, (slotId, slotConfig) ->
                if (index < uiSlots.get().size) {
                    val invSlot = uiSlots.get()[index]
                    val skill = template.getEquippedSkillByBackpackSlot(currentPageId, slotId)
                    if (skill != null) {
                        val icon = KeyBindingAPI.createIconFormatter(player, skill).build()
                        val append = equippedSkillAppend.get()
                        val meta = icon.itemMeta
                        if (meta != null && append.loreAppend.isNotEmpty()) {
                            val lore = meta.lore?.colored() ?: mutableListOf()
                            meta.lore = lore + append.loreAppend.colored()
                            icon.itemMeta = meta
                        }
                        set(invSlot, icon) {
                            if (clickEvent().isRightClick) {
                                BackpackAPI.unequipSkill(template, skill)
                                MinecraftInteraction.updateInventory(template)
                                BackpackUI.openTo(player)
                            }
                        }
                    } else {
                        val keybinding = Registries.KEYBINDING.getOrNull(slotConfig.key)
                        val icon = emptySlotIcon.get().clone()
                        val meta = icon.itemMeta
                        if (meta != null && meta.hasDisplayName()) {
                            meta.setDisplayName(meta.displayName.replace("{keyName}", keybinding?.name ?: slotConfig.key).colored())
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
                }
            }

            // 上一页
            if (currentIndex > 0) {
                set(18, prevPageIcon.get()) {
                    val prevPage = pageIds[currentIndex - 1]
                    BackpackAPI.setCurrentPage(template, prevPage)
                    MinecraftInteraction.updateInventory(template)
                    BackpackUI.openTo(player)
                }
            }

            // 下一页
            if (currentIndex < pageIds.size - 1) {
                set(26, nextPageIcon.get()) {
                    val nextPage = pageIds[currentIndex + 1]
                    BackpackAPI.setCurrentPage(template, nextPage)
                    MinecraftInteraction.updateInventory(template)
                    BackpackUI.openTo(player)
                }
            }

            // 页面信息
            val pageInfoItem = pageInfoIcon.get().clone()
            val pageInfoMeta = pageInfoItem.itemMeta
            if (pageInfoMeta != null && pageInfoMeta.hasDisplayName()) {
                pageInfoMeta.setDisplayName(
                    pageInfoMeta.displayName
                        .replace("{pageCurrent}", "${currentIndex + 1}")
                        .replace("{pageTotal}", "${pageIds.size}")
                        .colored()
                )
                pageInfoItem.itemMeta = pageInfoMeta
            }
            set(22, pageInfoItem) {}

            // 技能列表
            set(31, skillListIcon.get()) {
                BackpackSkillSelectUI.choice(player) { }
            }
        }
    }
}
