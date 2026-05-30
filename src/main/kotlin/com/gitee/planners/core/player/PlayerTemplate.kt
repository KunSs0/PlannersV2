package com.gitee.planners.core.player

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.common.metadata.MetadataContainer
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.config.Leveling
import com.gitee.planners.core.config.level.AlgorithmLevel
import com.gitee.planners.core.database.Database
import org.bukkit.entity.Player
import taboolib.common.platform.function.submitAsync
import java.util.concurrent.CompletableFuture

class PlayerTemplate(val id: Long, val onlinePlayer: Player, route: PlayerRoute?, map: Map<String, Metadata>) :
    MetadataContainer(map), Leveling {

    var route = route
        set(value) {
            if (value == null && field != null) {
                val skills = field!!.getImmutableSkillValues().mapNotNull { field!!.getSkillOrNull(it) }
                if (skills.isNotEmpty()) {
                    submitAsync {
                        Database.INSTANCE.deleteSkill(*skills.toTypedArray())
                    }
                }
            }
            field = value
            if (value != null) {
                val routerId = value.routerId
                if (playerRouter == null || playerRouter!!.routerId != routerId) {
                    playerRouter = Database.INSTANCE.loadPlayerRouter(id, routerId)
                    if (playerRouter == null) {
                        val initialLevel = value.router.algorithmLevel?.minLevel
                            ?: AlgorithmLevel.default?.minLevel
                            ?: 1
                        playerRouter = Database.INSTANCE.createPlayerRouter(id, routerId, initialLevel)
                    }
                }
            }
            submitAsync {
                Database.INSTANCE.updateRoute(this@PlayerTemplate)
            }
        }

    var playerRouter: PlayerRouter? = null
        private set

    val level: Int
        @JvmName("level0")
        get() {
            val pr = playerRouter
            if (pr != null) {
                return maxOf(pr.level, pr.minLevel)
            }
            val default = AlgorithmLevel.default
            if (default != null) {
                return default.minLevel
            }
            return 1
        }

    val experience: Int
        @JvmName("experience0")
        get() = playerRouter?.experience ?: 0

    val experienceMax: Int
        get() {
            val pr = playerRouter
            if (pr != null) {
                return pr.getExperienceMax(onlinePlayer)
            }
            return Int.MAX_VALUE
        }

    override fun getLevel(): Int = level

    override fun getExperience(): Int = experience

    override fun setLevel(level: Int) {
        val pr = playerRouter
        if (pr != null) {
            pr.setLevel(level, onlinePlayer)
        }
    }

    override fun addLevel(value: Int) {
        val pr = playerRouter
        if (pr != null) {
            pr.addLevel(value, onlinePlayer)
        }
    }

    override fun setExperience(experience: Int) {
        val pr = playerRouter
        if (pr != null) {
            pr.experience = experience
        }
    }

    override fun addExperience(value: Int): CompletableFuture<Void> {
        val pr = playerRouter
        if (pr != null) {
            return pr.addExperience(value, onlinePlayer)
        }
        return CompletableFuture.completedFuture(null)
    }

    override fun takeExperience(value: Int): CompletableFuture<Void> {
        val pr = playerRouter
        if (pr != null) {
            return pr.takeExperience(value, onlinePlayer)
        }
        return CompletableFuture.completedFuture(null)
    }

    fun getSkill(immutable: ImmutableSkill): CompletableFuture<PlayerSkill> {
        return getSkill(immutable.id)
    }

    /**
     * 获取一个技能 没有技能将创建技能实例
     */
    fun getSkill(id: String): CompletableFuture<PlayerSkill> {
        if (route == null) {
            error("You must specify a route")
        }
        if (!route!!.hasImmutableSkill(id)) {
            error("The skill $id does not exist in the route ${route!!.id}")
        }
        if (!route!!.hasSkill(id)) {
            return Database.INSTANCE.createPlayerSkill(this, route!!.getImmutableSkill(id)!!).thenApply {
                route!!.registerSkill(it)
                it
            }
        }
        return CompletableFuture.completedFuture(route!!.getSkillOrNull(id)!!)
    }

    fun getRegisteredSkillOrNull(id: String): PlayerSkill? {
        return route?.getSkillOrNull(id)
    }

    fun getEquippedSkillByBackpackSlot(page: String, slot: String): PlayerSkill? {
        return route?.getEquippedSkill(page, slot)
    }

    fun getEquippedSkillsForPage(page: String): Map<String, PlayerSkill?> {
        val pageConfig = com.gitee.planners.api.Registries.BACKPACK.getPage(page) ?: return emptyMap()
        val result = mutableMapOf<String, PlayerSkill?>()
        pageConfig.slots.keys.forEach { slotId ->
            result[slotId] = route?.getEquippedSkill(page, slotId)
        }
        return result
    }

    fun executeUpdatedDefaultSkill(): CompletableFuture<Void> {
        if (route != null) {
            return CompletableFuture.allOf(*route!!.getImmutableSkillValues().map { this.getSkill(it) }.toTypedArray())
        }
        return CompletableFuture.completedFuture(null)
    }

    /**
     * 获取已经注册的技能
     */
    fun getRegisteredSkill(): Map<String, PlayerSkill> {
        if (route == null) {
            error("You must specify a route")
        }
        return route!!.getRegisteredSkill()
    }

}
