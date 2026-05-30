package com.gitee.planners.core.skill.cooler

import com.gitee.planners.api.common.Unique
import com.gitee.planners.api.event.player.PlayerSkillCooldownEvent
import org.bukkit.entity.Player
import kotlin.math.min

class MemoryCooler : Cooler {

    private val map = mutableMapOf<String, Long>()

    override fun set(player: Player, skill: Unique, durationTick: Int) {
        val event = PlayerSkillCooldownEvent.Set(player, skill, durationTick)
        if (event.call()) {
            map["${player.uniqueId}-${skill.id}"] = event.ticks * 50 + System.currentTimeMillis()
        }

    }

    override fun get(player: Player, skill: Unique): Long {
        val path = "${player.uniqueId}-${skill.id}"
        if (map.containsKey(path)) {
            return maxOf(map[path]!! - System.currentTimeMillis(), 0) / 50
        }
        return 0
    }


}
