package com.gitee.planners.api.event.player

import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.core.player.PlayerSkill
import taboolib.platform.type.BukkitProxyEvent

abstract class PlayerSkillEvent(val template: PlayerTemplate, val skill: PlayerSkill) : BukkitProxyEvent() {

    val immutable: ImmutableSkill
        get() = skill.immutable

    val player = template.onlinePlayer

    class LevelChange(template: PlayerTemplate, skill: PlayerSkill, val form: Int, val to: Int) : PlayerSkillEvent(template, skill)

    /**
     * 玩家首次学习技能成功事件。
     *
     * 该事件仅在技能等级从 0 变为正数且等级写入完成后触发。
     *
     * @param template 玩家档案。
     * @param skill 已学习的技能。
     */
    class Learn(template: PlayerTemplate, skill: PlayerSkill) : PlayerSkillEvent(template, skill)

}
