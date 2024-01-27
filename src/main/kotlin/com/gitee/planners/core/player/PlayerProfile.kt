package com.gitee.planners.core.player

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.common.metadata.MetadataContainer
import com.gitee.planners.api.common.metadata.metadata
import com.gitee.planners.core.database.Database
import org.bukkit.entity.Player
import taboolib.common.platform.function.submitAsync

class PlayerProfile(val id: Long, val onlinePlayer: Player, route: PlayerRoute?, map: Map<String, Metadata>) : MetadataContainer(map) {

    var route = route
        set(value) {
            // 如果是要清空 route 则删除当前的技能
            if (value == null && field != null) {
                submitAsync {
                    val skills = field!!.getImmutableSkillValues().mapNotNull { field!!.getSkillOrNull(it) }
                    Database.INSTANCE.deleteSkill(*skills.toTypedArray())
                }
            }
            // 赋值
            field = value
            // 保存 route
            submitAsync {
                Database.INSTANCE.updateRoute(this@PlayerProfile)
            }
        }

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
        return route?.getSkillOrNull(id)
    }

    fun getPlayerSkills(): List<PlayerSkill> {
        if (route == null) return emptyList()
        return route!!.getImmutableSkillValues().mapNotNull { route!!.getSkillOrNull(it) as? PlayerSkill }
    }

}
