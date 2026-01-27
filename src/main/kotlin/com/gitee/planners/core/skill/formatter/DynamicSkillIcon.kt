package com.gitee.planners.core.skill.formatter

import com.gitee.planners.api.PlannersAPI
import com.gitee.planners.api.job.target.ProxyTarget
import com.gitee.planners.api.job.target.asTarget
import com.gitee.planners.module.fluxon.SingletonFluxonScript
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerSkill
import org.bukkit.entity.Player

class DynamicSkillIcon(sender: ProxyTarget<*>, skill: ImmutableSkill, level: Int = 1) :
    AbstractSkillIcon(sender, skill, level) {

    private val options by lazy {
        val player = sender.instance as? Player
        if (player != null) {
            PlannersAPI.newOptions(player, skill, level)
        } else {
            com.gitee.planners.module.fluxon.FluxonScriptOptions.forSkill(sender.instance ?: sender, level)
        }
    }

    override fun parse(text: String?): String {
        if (text == null) {
            return ""
        }
        return SingletonFluxonScript.replaceNested(text.trim(), options)
    }

    companion object {

        fun build(player: Player, skill: PlayerSkill) = build(player, skill.immutable, skill.level)

        fun build(player: Player, skill: ImmutableSkill, level: Int = 1) =
            DynamicSkillIcon(player.asTarget(), skill, level).build()

    }


}
