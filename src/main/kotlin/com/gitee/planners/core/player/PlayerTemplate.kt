package com.gitee.planners.core.player

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.common.metadata.MetadataContainer
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.config.Leveling
import com.gitee.planners.core.config.level.AlgorithmLevel
import com.gitee.planners.core.database.Database
import com.gitee.planners.api.event.player.PlayerLevelChangedEvent
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

    /**
     * 写入受职业算法约束的等级，并在提交后通知实际等级。
     *
     * @param level 请求设置的等级。
     */
    override fun setLevel(level: Int) {
        val router = playerRouter
        if (router != null) {
            // 必须在路由等级写入之后通知属性系统。
            val previous = this.level
            router.setLevel(level, onlinePlayer)
            publishLevelChanged(router, previous)
        }
    }

    /**
     * 增加等级，复用等级写入与提交后通知。
     *
     * @param value 要增加的等级数。
     */
    override fun addLevel(value: Int) {
        // 所有模板等级写入共用一个通知入口。
        setLevel(level + value)
    }

    override fun setExperience(experience: Int) {
        val router = playerRouter
        if (router != null) {
            router.experience = experience
        }
    }

    /**
     * 等待经验算法完成所有升级后发布一次实际等级变化。
     *
     * @param value 增加的经验值。
     * @return 包含提交后通知的完成阶段。
     */
    override fun addExperience(value: Int): CompletableFuture<Void> {
        val router = playerRouter
        if (router == null) {
            return CompletableFuture.completedFuture(null)
        }
        val previous = level
        // 等级算法可能异步完成，等待最终等级提交。
        return router.addExperience(value, onlinePlayer).thenRun {
            publishLevelChanged(router, previous)
        }
    }

    /**
     * 等待经验算法完成所有降级后发布实际等级变化。
     *
     * @param value 减少的经验值。
     * @return 包含提交后通知的完成阶段。
     */
    override fun takeExperience(value: Int): CompletableFuture<Void> {
        val router = playerRouter
        if (router == null) {
            return CompletableFuture.completedFuture(null)
        }
        val previous = level
        // 降级同样使用提交后语义，避免保留旧资源上限。
        return router.takeExperience(value, onlinePlayer).thenRun {
            publishLevelChanged(router, previous)
        }
    }

    /**
     * 只为仍绑定同一路由且等级实际变化的档案派发提交后事件。
     *
     * @param router 本次算法操作的路由。
     * @param previous 操作前等级。
     */
    private fun publishLevelChanged(router: PlayerRouter, previous: Int) {
        // 异步完成前转职时，不把旧路由结果广播给当前职业。
        if (playerRouter !== router || previous == level) {
            return
        }
        PlayerLevelChangedEvent(this, previous, level).call()
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
