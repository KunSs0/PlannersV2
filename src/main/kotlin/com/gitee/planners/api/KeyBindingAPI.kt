package com.gitee.planners.api

import com.gitee.planners.api.job.target.asTarget
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.formatter.DynamicSkillIcon
import com.gitee.planners.core.skill.formatter.IconFormatter
import com.gitee.planners.core.skilltree.SkillTreeNodeEffectService
import org.bukkit.entity.Player

object KeyBindingAPI {


    fun createIconFormatter(player: Player, skill: PlayerSkill): IconFormatter {
        val level = SkillTreeNodeEffectService.getSkillLevel(player.plannersTemplate, skill.id)
        return DynamicSkillIcon(player.asTarget(), skill.immutable, level)
    }

    fun createIconFormatter(player: Player,skill: PlayerSkill,level: Int): IconFormatter {
        return DynamicSkillIcon(player.asTarget(), skill.immutable, level)
    }

}
