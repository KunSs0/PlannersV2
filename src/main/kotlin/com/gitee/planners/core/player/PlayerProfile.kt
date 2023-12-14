package com.gitee.planners.core.player

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.common.metadata.MetadataContainer
import com.gitee.planners.api.common.metadata.metadata
import org.bukkit.entity.Player
import taboolib.common.platform.function.submitAsync

class PlayerProfile(val id: Long, val onlinePlayer: Player, var route: PlayerRoute?, map: Map<String, Metadata>) :
    MetadataContainer(map) {

    var level: Int
        get() = if (route == null) -1 else this["@job:level"]?.asInt() ?: 0
        set(value) {
            if (route != null) {
                this["@job:level"] = value.metadata()
            }
        }

    var experience: Int
        get() = if (route == null) -1 else this["@job:experience"]?.asInt() ?: 0
        set(value) {
            if (route != null) {
                this["@job:experience"] = value.metadata()
            }
        }


    fun getSkillOrNull(id: String): PlayerSkill? {
        return route?.getSkillOrNull(id) as? PlayerSkill
    }

    fun getPlayerSkills(): List<PlayerSkill> {
        if (route == null) return emptyList()
        return route!!.getImmutableSkillValues().mapNotNull { route!!.getSkillOrNull(it) as? PlayerSkill }
    }

    fun modifiedSkill(id: String, block: PlayerSkill.() -> Unit): PlayerSkill? {
        val skill = getSkillOrNull(id) ?: return null
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
