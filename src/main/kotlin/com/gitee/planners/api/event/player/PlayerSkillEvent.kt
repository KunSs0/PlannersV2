package com.gitee.planners.api.event.player

import com.gitee.planners.api.job.KeyBinding
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.core.player.PlayerSkill
import taboolib.platform.type.BukkitProxyEvent

abstract class PlayerSkillEvent(val template: PlayerTemplate, val skill: PlayerSkill) : BukkitProxyEvent() {

    val immutable: ImmutableSkill
        get() = skill.immutable

    val player = template.onlinePlayer

    class LevelChange(template: PlayerTemplate, skill: PlayerSkill, val form: Int, val to: Int) : PlayerSkillEvent(template, skill)

    class BindingChange(template: PlayerTemplate, skill: PlayerSkill, val binding: KeyBinding?) : PlayerSkillEvent(template, skill)

    class CoolChange(template: PlayerTemplate,skill: PlayerSkill,val cool: Int) : PlayerSkillEvent(template, skill)

}
