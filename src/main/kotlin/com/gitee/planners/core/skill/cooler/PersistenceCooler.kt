package com.gitee.planners.core.skill.cooler

import com.gitee.planners.api.common.metadata.metadataValue
import com.gitee.planners.api.event.player.PlayerSkillCooldownEvent
import com.gitee.planners.api.job.Skill
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.api.job.target.adaptTarget
import org.bukkit.entity.Player

class PersistenceCooler : Cooler {

    override fun set(player: Player, skill: Skill, durationTick: Int) {
        val event = PlayerSkillCooldownEvent.Set(player, skill, durationTick)
        if (event.call()) {
            val data = metadataValue(event.ticks * 50 + System.currentTimeMillis(), -1)
            adaptTarget<TargetBukkitEntity>(player).setMetadata(skill.id, data)
        }
    }

    override fun get(player: Player, skill: Skill): Long {
        val metadata = adaptTarget<TargetBukkitEntity>(player).getMetadata(skill.id)
        if (metadata == null) {
            return 0
        }
        return maxOf(metadata.asLong() - System.currentTimeMillis(), 0) / 50
    }


}
