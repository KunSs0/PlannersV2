package com.gitee.planners.core.player

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.common.metadata.MetadataContainer
import org.bukkit.entity.Player
import taboolib.common.platform.function.submitAsync

class PlayerProfile(val bindingId: Long, val onlinePlayer: Player, val router: PlayerRouter, val job: PlayerJob, map: Map<String, Metadata>) :
    MetadataContainer(map) {

    fun getSkill(id: String): PlayerSkill {
        return job.getSkillOrNull(id)!! as PlayerSkill
    }

    fun modifiedSkill(id: String, block: PlayerSkill.() -> Unit): PlayerSkill {
        val skill = getSkill(id)
        val cached = skill.level
        block(skill)
        if (cached != skill.level) {
            submitAsync {
                TODO("update to database")
            }
        }
        return skill
    }

}
