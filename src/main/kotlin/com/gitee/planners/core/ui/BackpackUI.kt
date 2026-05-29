package com.gitee.planners.core.ui

import com.gitee.planners.api.BackpackAPI
import com.gitee.planners.api.KeyBindingAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.Registries
import com.gitee.planners.core.skill.binding.MinecraftInteraction
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.util.replaceWithOrder
import taboolib.library.configuration.ConfigurationSection
import taboolib.library.xseries.XItemStack
import taboolib.platform.util.buildItem
import taboolib.platform.util.isRightClick

object BackpackUI : AutomationBaseUI("backpack.yml") {

    @Option("__option__.slots")
    val uiSlots = simpleConfigNodeTo<List<*>, List<Int>> { map { (it as Number).toInt() } }

    @Option("empty-slot")
    val emptySlotIcon = simpleConfigNodeTo<ConfigurationSection, ItemStack> { XItemStack.deserialize(this) }

    @Option("prev-page")
    val prevPageIcon = simpleConfigNodeTo<ConfigurationSection, ItemStack> { XItemStack.deserialize(this) }

    @Option("next-page")
    val nextPageIcon = simpleConfigNodeTo<ConfigurationSection, ItemStack> { XItemStack.deserialize(this) }

    @Option("page-info")
    val pageInfoIcon = simpleConfigNodeTo<ConfigurationSection, ItemStack> { XItemStack.deserialize(this) }

    @Option("skill-list")
    val skillListIcon = simpleConfigNodeTo<ConfigurationSection, ItemStack> { XItemStack.deserialize(this) }

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
                        set(invSlot, icon) {
                            if (clickEvent().isRightClick) {
                                BackpackAPI.unequipSkill(template, skill)
                                MinecraftInteraction.updateInventory(template)
                                BackpackUI.openTo(player)
                            }
                        }
                    } else {
                        val keybinding = Registries.KEYBINDING.getOrNull(slotConfig.key)
                        val icon = buildItem(emptySlotIcon.get()) {
                            name = name?.replaceWithOrder(keybinding?.name ?: slotConfig.key)
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
            set(22, buildItem(pageInfoIcon.get()) {
                name = name?.replaceWithOrder("${currentIndex + 1}", "${pageIds.size}")
            })

            // 技能列表
            set(31, skillListIcon.get()) {
                BackpackSkillSelectUI.choice(player) { }
            }
        }
    }
}
