package com.gitee.planners.core.skill.cooler

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.event.player.PlayerSkillEvent
import com.gitee.planners.api.job.Skill
import com.gitee.planners.core.player.PlayerSkill
import org.bukkit.entity.Player
import kotlin.math.min

class MemoryCooler : Cooler {

    private val map = mutableMapOf<String, Long>()

    override fun set(player: Player, skill: Skill, durationTick: Int) {
        map["${player.uniqueId}-${skill.id}"] = durationTick * 50 + System.currentTimeMillis()
        (skill as? PlayerSkill)?.let {skill ->
            PlayerSkillEvent.CoolChange(player.plannersTemplate,skill,durationTick).call()
        }
    }

    override fun get(player: Player, skill: Skill): Long {
        val path = "${player.uniqueId}-${skill.id}"
        if (map.containsKey(path)) {
            return maxOf(map[path]!! - System.currentTimeMillis(), 0) / 50
        }
        return 0
    }


}
