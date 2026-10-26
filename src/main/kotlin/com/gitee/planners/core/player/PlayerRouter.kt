package com.gitee.planners.core.player

import com.gitee.planners.api.Registries
import com.gitee.planners.core.config.ImmutableJob
import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.config.ImmutableRouter
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.config.level.Algorithm
import com.gitee.planners.core.config.level.AlgorithmLevel
import com.gitee.planners.core.database.ProfilePersistence
import com.gitee.planners.core.database.RecordIdentity
import com.gitee.planners.core.condition.ConditionMainThread
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common5.cfloat
import java.util.concurrent.CompletableFuture

/**
 * 玩家在一个 Router 下的完整转职线。
 *
 * PlayerRoute 保存单个 Job 阶段；本类负责父链解析、共享等级/SP 和跨阶段技能聚合。
 */
class PlayerRouter(
    val bindingId: Long,
    val userId: Long,
    val routerId: String,
    initialLevel: Int,
    initialExperience: Int,
    initialCurrentRouteId: Long,
    initialSkillPointsCurrent: Int,
    initialSkillPointsUsed: Int,
    routes: Collection<PlayerRoute>
) {

    /** 独立的持久化身份，只由后台 SQL 消费者解析。 */
    val persistenceId = RecordIdentity(bindingId)

    val router: ImmutableRouter
        get() = Registries.ROUTER.getOrNull(routerId) ?: error("Could not find router with id '$routerId'")

    val algorithm: Algorithm?
        get() = router.algorithmLevel ?: AlgorithmLevel.default

    private val routesById = LinkedHashMap<Long, PlayerRoute>()

    private val equippedByPageSlot = LinkedHashMap<String, PlayerSkill>()

    var level = initialLevel
        set(value) {
            field = value
            save()
        }

    var experience = initialExperience
        set(value) {
            field = value
            save()
        }

    var currentRouteId = initialCurrentRouteId
        private set

    var skillPointsCurrent = initialSkillPointsCurrent
        private set

    var skillPointsUsed = initialSkillPointsUsed
        private set

    init {
        for (route in routes) {
            registerRoute(route)
        }
        if (currentRouteId != -1L) {
            rebuildEquippedIndex()
        }
    }

    val currentRoute: PlayerRoute
        get() {
            val route = routesById[currentRouteId]
            if (route == null) {
                error("PlayerRouter '$bindingId' 未找到当前 PlayerRoute '$currentRouteId'")
            }
            return route
        }

    val routeLine: List<PlayerRoute>
        get() {
            val result = mutableListOf<PlayerRoute>()
            val visited = mutableSetOf<Long>()
            var route = currentRoute
            while (true) {
                if (!visited.add(route.bindingId)) {
                    error("PlayerRouter '$bindingId' 的转职线存在循环")
                }
                result.add(route)
                if (route.parentId == -1L) {
                    break
                }
                val parent = routesById[route.parentId]
                if (parent == null) {
                    error("PlayerRouter '$bindingId' 缺少父 PlayerRoute '${route.parentId}'")
                }
                route = parent
            }
            result.reverse()
            return result
        }

    val effectiveSkills: Map<String, PlayerSkill>
        get() {
            val result = LinkedHashMap<String, PlayerSkill>()
            for (route in routeLine) {
                for ((skillId, skill) in route.getRegisteredSkill()) {
                    val previous = result.put(skillId, skill)
                    if (previous != null) {
                        error("PlayerRouter '$bindingId' 的转职线存在重复技能 '$skillId'")
                    }
                }
            }
            return result
        }

    fun getRoutes(): Collection<PlayerRoute> {
        return routesById.values
    }

    fun registerRoute(route: PlayerRoute) {
        if (route.routerId != routerId) {
            error("PlayerRoute '${route.bindingId}' 不属于 Router '$routerId'")
        }
        routesById[route.bindingId] = route
    }

    fun setCurrentRoute(route: PlayerRoute) {
        if (!routesById.containsKey(route.bindingId)) {
            error("PlayerRoute '${route.bindingId}' 未注册到 PlayerRouter '$bindingId'")
        }
        currentRouteId = route.bindingId
        rebuildEquippedIndex()
    }

    fun getCurrentJob(): ImmutableJob {
        return currentRoute.getJob()
    }

    fun getNextRoutes(): List<ImmutableRoute> {
        return currentRoute.getBranches()
    }

    fun hasJobInLine(jobId: String): Boolean {
        for (route in routeLine) {
            if (route.jobId == jobId) {
                return true
            }
        }
        return false
    }

    fun getRouteForSkill(skillId: String): PlayerRoute? {
        for (route in routeLine) {
            if (route.hasImmutableSkill(skillId)) {
                return route
            }
        }
        return null
    }

    fun hasImmutableSkill(skillId: String): Boolean {
        return getRouteForSkill(skillId) != null
    }

    fun getImmutableSkill(skillId: String): ImmutableSkill? {
        val route = getRouteForSkill(skillId)
        if (route == null) {
            return null
        }
        return route.getImmutableSkill(skillId)
    }

    fun getImmutableSkillValues(): List<ImmutableSkill> {
        val result = mutableListOf<ImmutableSkill>()
        for (route in routeLine) {
            result.addAll(route.getImmutableSkillValues())
        }
        return result
    }

    fun getSkillOrNull(skillId: String): PlayerSkill? {
        return effectiveSkills[skillId]
    }

    fun getEquippedSkill(page: String, slot: String): PlayerSkill? {
        return equippedByPageSlot["$page:$slot"]
    }

    fun updateEquippedIndex(skill: PlayerSkill) {
        val iterator = equippedByPageSlot.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value == skill) {
                iterator.remove()
            }
        }
        if (skill.equipped && skill.backpackPage != null && skill.backpackSlot != null) {
            equippedByPageSlot["${skill.backpackPage}:${skill.backpackSlot}"] = skill
        }
    }

    private fun rebuildEquippedIndex() {
        equippedByPageSlot.clear()
        for (route in routeLine) {
            for (skill in route.getRegisteredSkill().values) {
                updateEquippedIndex(skill)
            }
        }
    }

    /**
     * 在业务线程调整职业线技能点，然后提交不可变保存快照。
     * @param amount 要增加的点数，减少时沿用现有非负余额规则。
     */
    fun addSkillPoints(amount: Int) {
        // 后台只读取捕获后的字段，不与这里争用在线对象锁。
        skillPointsCurrent = maxOf(0, Math.addExact(skillPointsCurrent, amount))
        save()
    }

    /**
     * 将当前等级应得的累计技能点与已发放总量对齐。
     *
     * 已发放总量由可用余额与累计消耗推导，重复调用不会重复补发；
     * 已消费点数保持不变，只补足目标累计值与已发放总量之间的差额。
     *
     * @param targetTotal 当前等级对应的累计技能点。
     * @return 本次实际补发的点数。
     */
    fun reconcileSkillPoints(targetTotal: Int): Int {
        // 主线程串行变更点数，持久化只消费当前结果。
        val delta = targetTotal - skillPointsCurrent - skillPointsUsed
        if (delta <= 0) {
            return 0
        }
        skillPointsCurrent += delta
        save()
        return delta
    }

    /**
     * 直接消费职业线技能点并安排保存；节点学习在内存提交后批量持久化。
     * @param amount 需要扣除的技能点。
     * @return 当前余额是否足够完成扣除。
     */
    fun takeSkillPoints(amount: Int): Boolean {
        // 业务线程直接检查余额，不等待数据库或后台保存。
        require(amount >= 0) { "技能点成本不能为负数" }
        if (skillPointsCurrent < amount) {
            return false
        }
        val nextUsed = Math.addExact(skillPointsUsed, amount)
        skillPointsCurrent -= amount
        skillPointsUsed = nextUsed
        save()
        return true
    }

    /**
     * 同步发布学习扣费后的技能点，随后与节点一起提交后台快照。
     * @param amount 本次已提交的非负成本。
     */
    internal fun publishLearningPoints(amount: Int) {
        // 调用方已在主线程检查余额与溢出，不与任何 SQL 任务共用锁。
        skillPointsCurrent -= amount
        skillPointsUsed += amount
    }

    fun getExperienceMax(player: Player): Int {
        val algo = algorithm
        if (algo == null) {
            return Int.MAX_VALUE
        }
        return algo.getExp(player, level).getNow(Int.MAX_VALUE)
    }

    val minLevel: Int
        get() = algorithm?.minLevel ?: 1

    val maxLevel: Int
        get() = algorithm?.maxLevel ?: Int.MAX_VALUE

    fun setLevel(level: Int, player: Player) {
        val algo = algorithm
        if (algo == null) {
            this.level = level
            return
        }
        val expMax = getExperienceMax(player)
        val progress = maxOf(0f, minOf(1.0f, experience / expMax.cfloat))
        this.level = maxOf(algo.minLevel, minOf(algo.maxLevel, level))
        this.experience = (progress * getExperienceMax(player)).toInt()
    }

    fun addLevel(value: Int, player: Player) {
        setLevel(level + value, player)
    }

    /**
     * 异步计算升级阈值，回主线程提交等级、经验和保存快照。
     * @param value 增加的经验。
     * @param player 当前玩家。
     * @return 内存算法完成阶段，不等待数据库。
     */
    fun addExperience(value: Int, player: Player): CompletableFuture<Void> {
        // 进入主线程只是调度续体，调用线程不等待。
        if (!Bukkit.isPrimaryThread()) {
            return CompletableFuture.completedFuture(Unit).thenComposeAsync({ addExperience(value, player) }, ConditionMainThread)
        }
        val algo = algorithm
        if (algo == null) {
            experience += value
            return CompletableFuture.completedFuture(null)
        }
        if (level >= algo.maxLevel) {
            level = algo.maxLevel
            return algo.getExp(player, level).thenAcceptAsync({
                experience = it
            }, ConditionMainThread)
        }
        val future = CompletableFuture<Void>()
        var currentLevel = level
        var currentExperience = experience + value

        /** 在主线程发布一次完整算法结果。 */
        fun finish() {
            level = currentLevel
            experience = currentExperience
            future.complete(null)
        }

        /** 等待阈值只注册续体，不调用 join/get。 */
        fun process() {
            algo.getExp(player, currentLevel).thenAcceptAsync({ requiredExperience ->
                if (currentExperience >= requiredExperience && currentLevel < algo.maxLevel) {
                    currentLevel += 1
                    currentExperience -= requiredExperience
                    process()
                } else {
                    finish()
                }
            }, ConditionMainThread).exceptionally { failure ->
                future.completeExceptionally(failure)
                null
            }
        }
        process()
        return future
    }

    /**
     * 异步计算降级阈值，回主线程提交等级、经验和保存快照。
     * @param value 减少的经验。
     * @param player 当前玩家。
     * @return 内存算法完成阶段，不等待数据库。
     */
    fun takeExperience(value: Int, player: Player): CompletableFuture<Void> {
        // 统一业务线程所有权，后台 SQL 只负责持久化。
        if (!Bukkit.isPrimaryThread()) {
            return CompletableFuture.completedFuture(Unit).thenComposeAsync({ takeExperience(value, player) }, ConditionMainThread)
        }
        val algo = algorithm
        if (algo == null) {
            experience = maxOf(experience - value, 0)
            return CompletableFuture.completedFuture(null)
        }
        if (level <= algo.minLevel) {
            level = algo.minLevel
            experience = maxOf(experience - value, 0)
            return CompletableFuture.completedFuture(null)
        }
        val future = CompletableFuture<Void>()
        var currentLevel = level
        var currentExperience = experience - value

        /** 在主线程发布一次完整降级结果。 */
        fun finish() {
            level = currentLevel
            experience = maxOf(currentExperience, 0)
            future.complete(null)
        }

        /** 计算下一阈值，完成或失败都结束请求，不悬挂 Future。 */
        fun process() {
            if (currentExperience >= 0 || currentLevel <= algo.minLevel) {
                finish()
                return
            }
            algo.getExp(player, currentLevel - 1).thenAcceptAsync({ previousLevelExperience ->
                currentLevel -= 1
                currentExperience += previousLevelExperience
                process()
            }, ConditionMainThread).exceptionally { failure ->
                future.completeExceptionally(failure)
                null
            }
        }
        process()
        return future
    }

    /** 捕获当前字段后立即返回，后台不再读取此对象。 */
    private fun save() {
        ProfilePersistence.saveRouter(this)
    }
}
