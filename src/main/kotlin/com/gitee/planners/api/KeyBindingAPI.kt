package com.gitee.planners.api

import com.gitee.planners.api.job.target.asTarget
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.formatter.DynamicSkillIcon
import com.gitee.planners.core.skill.formatter.IconFormatter
import org.bukkit.entity.Player

object KeyBindingAPI {


    fun createIconFormatter(player: Player, skill: PlayerSkill): IconFormatter {
        return DynamicSkillIcon(player.asTarget(), skill.immutable, skill.level)
    }

    fun createIconFormatter(player: Player,skill: PlayerSkill,level: Int): IconFormatter {
        return DynamicSkillIcon(player.asTarget(), skill.immutable, level)
    }

}
