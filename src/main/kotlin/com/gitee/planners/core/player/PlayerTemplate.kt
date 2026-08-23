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

class PlayerTemplate(
    val id: Long,
    val onlinePlayer: Player,
    initialPlayerRouter: PlayerRouter?,
    map: Map<String, Metadata>
) : MetadataContainer(map), Leveling {

    var playerRouter = initialPlayerRouter

    fun clearPlayerRouter() {
        val router = playerRouter
        if (router == null) {
            return
        }
        playerRouter = null
        submitAsync {
            Database.INSTANCE.deletePlayerRouter(router)
        }
    }

    val level: Int
        @JvmName("level0")
        get() {
            val router = playerRouter
            if (router != null) {
                return maxOf(router.level, router.minLevel)
            }
            val default = AlgorithmLevel.default
            if (default != null) {
                return default.minLevel
            }
            return 1
        }

    val experience: Int
        @JvmName("experience0")
        get() {
            val router = playerRouter
            if (router == null) {
                return 0
            }
            return router.experience
        }

    val experienceMax: Int
        get() {
            val router = playerRouter
            if (router != null) {
                return router.getExperienceMax(onlinePlayer)
            }
            return Int.MAX_VALUE
        }

    override fun getLevel(): Int {
        return level
    }

    override fun getExperience(): Int {
        return experience
    }

    override fun setLevel(level: Int) {
        val router = playerRouter
        if (router != null) {
            router.setLevel(level, onlinePlayer)
        }
    }

    override fun addLevel(value: Int) {
        val router = playerRouter
        if (router != null) {
            router.addLevel(value, onlinePlayer)
        }
    }

    override fun setExperience(experience: Int) {
        val router = playerRouter
        if (router != null) {
            router.experience = experience
        }
    }

    override fun addExperience(value: Int): CompletableFuture<Void> {
        val router = playerRouter
        if (router == null) {
            return CompletableFuture.completedFuture(null)
        }
        return router.addExperience(value, onlinePlayer)
    }

    override fun takeExperience(value: Int): CompletableFuture<Void> {
        val router = playerRouter
        if (router == null) {
            return CompletableFuture.completedFuture(null)
        }
        return router.takeExperience(value, onlinePlayer)
    }

    fun getSkill(immutable: ImmutableSkill): CompletableFuture<PlayerSkill> {
        return getSkill(immutable.id)
    }

    fun getSkill(id: String): CompletableFuture<PlayerSkill> {
        val router = playerRouter
        if (router == null) {
            error("You must specify a player router")
        }
        val route = router.getRouteForSkill(id)
        if (route == null) {
            error("The skill $id does not exist in router ${router.routerId}")
        }
        val existing = router.getSkillOrNull(id)
        if (existing != null) {
            return CompletableFuture.completedFuture(existing)
        }
        val immutableSkill = route.getImmutableSkill(id)
        if (immutableSkill == null) {
            error("The skill $id does not exist in route ${route.jobId}")
        }
        return Database.INSTANCE.createPlayerSkill(this, route, immutableSkill).thenApply { playerSkill ->
            route.registerSkill(playerSkill)
            router.updateEquippedIndex(playerSkill)
            playerSkill
        }
    }

    fun getRegisteredSkillOrNull(id: String): PlayerSkill? {
        val router = playerRouter
        if (router == null) {
            return null
        }
        return router.getSkillOrNull(id)
    }

    fun getEquippedSkillByBackpackSlot(page: String, slot: String): PlayerSkill? {
        val router = playerRouter
        if (router == null) {
            return null
        }
        return router.getEquippedSkill(page, slot)
    }

    fun getEquippedSkillsForPage(page: String): Map<String, PlayerSkill?> {
        val pageConfig = com.gitee.planners.api.Registries.BACKPACK.getPage(page)
        if (pageConfig == null) {
            return emptyMap()
        }
        val result = mutableMapOf<String, PlayerSkill?>()
        for (slotId in pageConfig.slots.keys) {
            result[slotId] = getEquippedSkillByBackpackSlot(page, slotId)
        }
        return result
    }

    fun executeUpdatedDefaultSkill(): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }

    fun getRegisteredSkill(): Map<String, PlayerSkill> {
        val router = playerRouter
        if (router == null) {
            error("You must specify a player router")
        }
        return router.effectiveSkills
    }
}
