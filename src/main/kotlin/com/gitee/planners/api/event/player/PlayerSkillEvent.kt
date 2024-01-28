package com.gitee.planners.api.event.player

import com.gitee.planners.api.job.KeyBinding
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerProfile
import com.gitee.planners.core.player.PlayerSkill
import taboolib.platform.type.BukkitProxyEvent

abstract class PlayerSkillEvent(val profile: PlayerProfile, val skill: PlayerSkill) : BukkitProxyEvent() {

    val immutable: ImmutableSkill
        get() = skill.immutable

    val player = profile.onlinePlayer

    class LevelChange(profile: PlayerProfile, skill: PlayerSkill, val form: Int, val to: Int) : PlayerSkillEvent(profile, skill)

    class BindingChange(profile: PlayerProfile,skill: PlayerSkill,val binding: KeyBinding?) : PlayerSkillEvent(profile, skill)

}
