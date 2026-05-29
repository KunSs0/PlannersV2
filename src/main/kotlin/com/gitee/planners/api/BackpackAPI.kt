package com.gitee.planners.api

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.metadata.metadataValue
import com.gitee.planners.api.event.player.BackpackEquipEvent
import com.gitee.planners.api.event.player.BackpackPageSwitchEvent
import com.gitee.planners.core.database.Database
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.player.PlayerTemplate
import org.bukkit.entity.Player
import taboolib.common.platform.function.submitAsync

object BackpackAPI {

    /** 获取玩家当前页面 ID，首次取 default-page */
    fun getCurrentPage(template: PlayerTemplate): String {
        return template["backpack_current_page"]?.asString()
            ?: Registries.BACKPACK.defaultPage
    }

    /** 设置玩家当前页面 */
    fun setCurrentPage(template: PlayerTemplate, page: String) {
        Registries.BACKPACK.getPage(page) ?: return
        val prev = getCurrentPage(template)
        if (BackpackPageSwitchEvent.Pre(template, prev, page).call()) {
            template["backpack_current_page"] = metadataValue(page)
            BackpackPageSwitchEvent.Post(template, prev, page).call()
        }
    }

    /** 装备技能到指定页面的槽位 */
    fun equipSkill(template: PlayerTemplate, skill: PlayerSkill, page: String, slot: String) {
        val event = BackpackEquipEvent.Equip(template, skill, page, slot)
        if (!event.call()) return

        // 如果该槽位已有技能，先卸下
        val existing = template.getEquippedSkillByBackpackSlot(page, slot)
        if (existing != null && existing != skill) {
            unequipSkill(template, existing)
        }
        // 如果该技能已在其他槽位装备，先卸下
        if (skill.equipped) {
            unequipSkill(template, skill)
        }

        skill.equipped = true
        skill.backpackPage = page
        skill.backpackSlot = slot
        template.route?.updateEquippedIndex(skill)
        submitAsync { Database.INSTANCE.updateSkill(skill) }
    }

    /** 卸下技能 */
    fun unequipSkill(template: PlayerTemplate, skill: PlayerSkill) {
        if (!skill.equipped) return
        val event = BackpackEquipEvent.Unequip(template, skill, skill.backpackPage, skill.backpackSlot)
        if (!event.call()) return

        skill.equipped = false
        skill.backpackPage = null
        skill.backpackSlot = null
        template.route?.updateEquippedIndex(skill)
        submitAsync { Database.INSTANCE.updateSkill(skill) }
    }

    /** 按按键 ID 查找技能（用于释放链路） */
    fun getSkillByKey(template: PlayerTemplate, keyId: String): PlayerSkill? {
        val currentPage = getCurrentPage(template)
        val pageConfig = Registries.BACKPACK.getPage(currentPage) ?: return null
        val slot = pageConfig.getSlotForKey(keyId) ?: return null
        return template.getEquippedSkillByBackpackSlot(currentPage, slot.id)
    }

    /** 获取按键对应的 hotbar 索引 */
    fun getHotbarIndex(keyId: String): Int {
        return Registries.KEYBINDING.values().indexOfFirst { it.id == keyId }
    }
}
