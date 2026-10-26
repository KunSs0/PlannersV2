package com.gitee.planners.core.player

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.common.metadata.MetadataContainer
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.config.Leveling
import com.gitee.planners.core.config.level.AlgorithmLevel
import com.gitee.planners.core.database.ProfilePersistence
import com.gitee.planners.core.database.RecordIdentity
import com.gitee.planners.core.condition.ConditionMainThread
import com.gitee.planners.api.event.player.PlayerLevelChangedEvent
import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.event.player.PlayerRouteClearedEvent
import com.gitee.planners.core.attribute.AttributeProxy
import com.gitee.planners.core.skill.directing.DirectingSessionManager
import com.gitee.planners.core.skill.timing.SkillCastingSessions
import com.gitee.planners.module.compat.attribute.AttributeDriver
import com.gitee.planners.module.compat.attribute.AttributeProvider
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * 玩家在线档案及其当前职业线的统一访问入口。
 *
 * @property id 玩家档案的持久化 ID。
 * @property onlinePlayer 档案所属的在线玩家。
 * @param initialPlayerRouter 加载时绑定的职业线，无职业时为 null。
 * @param map 加载的档案元数据。
 */
class PlayerTemplate(
    val id: Long,
    val onlinePlayer: Player,
    initialPlayerRouter: PlayerRouter?,
    map: Map<String, Metadata>
) : MetadataContainer(map), Leveling {

    var playerRouter = initialPlayerRouter

    /**
     * 在主线程提交职业清空、取消未完成释放并重算属性，随后异步持久化。
     *
     * 完成事件表示内存和属性已经提交，不等待数据库删除；持久化失败独立记录，
     * 不恢复旧职业。重复清空不再次通知或删除数据。
     *
     * @throws IllegalStateException 调用线程、档案身份或属性系统未就绪时抛出。
     */
    fun clearPlayerRouter() {
        // 在任何状态变更前拒绝异步调用、退出玩家和已经被替换的档案。
        check(Bukkit.isPrimaryThread()) { "清空职业必须在 Bukkit 主线程执行" }
        check(onlinePlayer.isOnline && PlayerTemplateAPI.getOrNull(onlinePlayer.uniqueId) === this) {
            "清空职业需要当前在线且已就绪的玩家档案"
        }
        val router = playerRouter
        if (router == null) {
            return
        }
        check(!AttributeProvider.reloading) { "属性系统重载期间不能清空职业" }
        checkNotNull(AttributeDriver.current()) { "清空职业前必须注册属性驱动" }
        // 先断开有效技能入口，再取消持有旧技能续体的会话。
        playerRouter = null
        SkillCastingSessions.cancel(onlinePlayer)
        DirectingSessionManager.cancel(onlinePlayer)
        // 只替换 Planners 的聚合来源，装备等其他来源继续由属性系统保留。
        AttributeProxy.sync(onlinePlayer)
        ProfilePersistence.deleteRouter(router) {
            PlayerRouteClearedEvent(this, router.bindingId, router.routerId).call()
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

    /**
     * 在主线程复用或创建技能并立即注册，后台 INSERT 不决定业务完成时机。
     * @param id 职业线中的技能配置 ID。
     * @return 已注册的运行时技能，不等待生成数据库主键。
     */
    fun getSkill(id: String): CompletableFuture<PlayerSkill> {
        return CompletableFuture.supplyAsync({
            check(onlinePlayer.isOnline && PlayerTemplateAPI.getOrNull(onlinePlayer.uniqueId) === this) { "玩家档案已经失效" }
            val router = checkNotNull(playerRouter) { "玩家尚未选择职业线" }
            val route = checkNotNull(router.getRouteForSkill(id)) { "技能 $id 不属于当前职业线" }
            val existing = router.getSkillOrNull(id)
            if (existing != null) {
                existing
            } else {
                val immutable = checkNotNull(route.getImmutableSkill(id)) { "职业阶段没有技能 $id" }
                val skill = PlayerSkill(RecordIdentity.next(), immutable.id, immutable.startedLevel)
                // 先注册，重复请求立即复用同一实例，后台不回填在线集合。
                route.registerSkill(skill)
                router.updateEquippedIndex(skill)
                ProfilePersistence.createSkill(route, skill)
                skill
            }
        }, ConditionMainThread)
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

    fun getRegisteredSkill(): Map<String, PlayerSkill> {
        val router = playerRouter
        if (router == null) {
            error("You must specify a player router")
        }
        return router.effectiveSkills
    }
}
