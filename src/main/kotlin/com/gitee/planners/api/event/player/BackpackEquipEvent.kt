package com.gitee.planners.api.event.player

import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.player.PlayerTemplate
import taboolib.platform.type.BukkitProxyEvent

abstract class BackpackEquipEvent(
    val template: PlayerTemplate,
    val skill: PlayerSkill,
    val page: String?,
    val slot: String?
) : BukkitProxyEvent() {

    val player = template.onlinePlayer

    class Equip(template: PlayerTemplate, skill: PlayerSkill, page: String, slot: String) :
        BackpackEquipEvent(template, skill, page, slot)

    /**
     * 技能装配成功事件。
     *
     * 事件触发时，技能的装备状态、页面、槽位和路线索引均已更新。
     *
     * @param template 玩家档案。
     * @param skill 已装配的技能。
     * @param page 技能栏页面 ID。
     * @param slot 技能栏槽位 ID。
     */
    class Post(template: PlayerTemplate, skill: PlayerSkill, page: String, slot: String) :
        BackpackEquipEvent(template, skill, page, slot)

    class Unequip(template: PlayerTemplate, skill: PlayerSkill, page: String?, slot: String?) :
        BackpackEquipEvent(template, skill, page, slot)
}
