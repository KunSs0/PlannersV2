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

    class Unequip(template: PlayerTemplate, skill: PlayerSkill, page: String?, slot: String?) :
        BackpackEquipEvent(template, skill, page, slot)
}
