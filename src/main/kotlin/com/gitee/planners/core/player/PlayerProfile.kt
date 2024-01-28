package com.gitee.planners.core.player

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.common.metadata.MetadataContainer
import com.gitee.planners.api.common.metadata.metadata
import com.gitee.planners.api.common.registry.Registry
import com.gitee.planners.api.event.player.PlayerSkillEvent
import com.gitee.planners.api.job.KeyBinding
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.database.Database
import org.bukkit.entity.Player
import taboolib.common.platform.function.submitAsync
import java.util.concurrent.CompletableFuture

class PlayerProfile(val id: Long, val onlinePlayer: Player, route: PlayerRoute?, map: Map<String, Metadata>) :
    MetadataContainer(map) {

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

    fun getSkill(immutable: ImmutableSkill): CompletableFuture<PlayerSkill> {
        return getSkill(immutable.id)
    }

    /**
     * 获取一个技能 没有技能将创建技能实例
     */
    fun getSkill(id: String): CompletableFuture<PlayerSkill> {
        // 如果 route 不存在
        if (route == null) {
            error("You must specify a route")
        }
        // 如果Immutable Skill 不存在这个技能
        if (route!!.hasImmutableSkill(id)) {
            error("Skill $id already exists for ${route!!.id}")
        }
        // 如果没有学习 则添加一个新的（这时候并未在数据库内添加）
        if (!route!!.hasSkill(id)) {
            Database.INSTANCE.createPlayerSkill(this, route!!.getImmutableSkill(id)!!).thenApply {
                route!!.registerSkill(it)
                it
            }
        }
        return CompletableFuture.completedFuture(route!!.getSkillOrNull(id)!!)
    }


    fun getRegistriedSkillOrNull(id: String): PlayerSkill? {
        return route?.getSkillOrNull(id)
    }

    fun getRegistriedSkillOrNull(binding: KeyBinding): PlayerSkill? {
        return getRegistrySkill().getValues().firstOrNull { it.binding == binding }
    }

    fun executeUpdatedDefaultSkill() : CompletableFuture<Void> {
        if (route != null) {
            return CompletableFuture.allOf(*route!!.getImmutableSkillValues().map { this.getSkill(it) }.toTypedArray())
        }
        return CompletableFuture.completedFuture(null)
    }

    /**
     * 获取已经注册的技能
     */
    fun getRegistrySkill(): Registry<String, PlayerSkill> {
        // 如果 route 不存在
        if (route == null) {
            error("You must specify a route")
        }
        return route!!.getRegistrySkill()
    }

}
