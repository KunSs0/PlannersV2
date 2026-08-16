package com.gitee.planners.core.player

import com.gitee.planners.api.Registries
import com.gitee.planners.core.config.ImmutableJob
import com.gitee.planners.core.config.ImmutableRoute
import com.gitee.planners.core.config.ImmutableRouter
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.config.level.Algorithm
import com.gitee.planners.core.config.level.AlgorithmLevel
import com.gitee.planners.core.database.Database
import org.bukkit.entity.Player
import taboolib.common.platform.function.submitAsync
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
        if (currentRouteId >= 0) {
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
                if (route.parentId < 0) {
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
        save()
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

    fun addSkillPoints(amount: Int) {
        skillPointsCurrent = maxOf(0, skillPointsCurrent + amount)
        save()
    }

    fun takeSkillPoints(amount: Int): Boolean {
        if (skillPointsCurrent < amount) {
            return false
        }
        skillPointsCurrent -= amount
        skillPointsUsed += amount
        save()
        return true
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

    fun addExperience(value: Int, player: Player): CompletableFuture<Void> {
        val algo = algorithm
        if (algo == null) {
            experience += value
            return CompletableFuture.completedFuture(null)
        }
        if (level >= algo.maxLevel) {
            level = algo.maxLevel
            algo.getExp(player, level).thenAccept {
                experience = it
            }
            return CompletableFuture.completedFuture(null)
        }
        val future = CompletableFuture<Void>()
        var currentLevel = level
        var currentExperience = experience + value

        fun finish() {
            level = currentLevel
            experience = currentExperience
            future.complete(null)
        }

        fun process() {
            algo.getExp(player, currentLevel).thenAccept { requiredExperience ->
                if (currentExperience >= requiredExperience && currentLevel < algo.maxLevel) {
                    currentLevel += 1
                    currentExperience -= requiredExperience
                    process()
                } else {
                    finish()
                }
            }
        }
        process()
        return future
    }

    fun takeExperience(value: Int, player: Player): CompletableFuture<Void> {
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

        fun finish() {
            level = currentLevel
            experience = maxOf(currentExperience, 0)
            future.complete(null)
        }

        fun process() {
            if (currentExperience >= 0 || currentLevel <= algo.minLevel) {
                finish()
                return
            }
            algo.getExp(player, currentLevel - 1).thenAccept { previousLevelExperience ->
                currentLevel -= 1
                currentExperience += previousLevelExperience
                process()
            }
        }
        process()
        return future
    }

    private fun save() {
        submitAsync {
            Database.INSTANCE.updatePlayerRouter(this@PlayerRouter)
        }
    }
}
