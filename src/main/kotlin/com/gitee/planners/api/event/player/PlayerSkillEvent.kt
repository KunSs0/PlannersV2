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

}
