package com.gitee.planners.core.skill.cooler

import com.gitee.planners.api.common.metadata.MetadataTypeToken
import com.gitee.planners.api.common.metadata.createMetadata
import com.gitee.planners.api.job.Skill
import com.gitee.planners.api.job.target.Target
import com.gitee.planners.api.job.target.TargetBukkitEntity
import com.gitee.planners.api.job.target.adaptTarget
import org.bukkit.entity.Player

class PersistenceCooler : Cooler {

    override fun set(player: Player, skill: Skill, durationTick: Int) {
        val data = createMetadata(durationTick * 50 + System.currentTimeMillis(), -1)
        adaptTarget<TargetBukkitEntity>(player).setMetadata(skill.id, data)
    }

    override fun get(player: Player, skill: Skill): Long {
        val metadata = adaptTarget<TargetBukkitEntity>(player).getMetadata(skill.id)
        if (metadata == null) {
            return -1
        }
        return metadata.asLong()
    }


}
