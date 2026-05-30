package com.gitee.planners.core.player

import com.gitee.planners.api.Registries
import com.gitee.planners.core.config.ImmutableRouter
import com.gitee.planners.core.config.level.Algorithm
import com.gitee.planners.core.config.level.AlgorithmLevel
import com.gitee.planners.core.database.Database
import org.bukkit.entity.Player
import taboolib.common.platform.function.submitAsync
import taboolib.common5.cfloat
import java.util.concurrent.CompletableFuture

class PlayerRouter(
    val bindingId: Long,
    val routerId: String,
    initialLevel: Int,
    initialExperience: Int
) {

    val router: ImmutableRouter
        get() = Registries.ROUTER.getOrNull(routerId) ?: error("Could not find router with id '$routerId'")

    private val algorithm: Algorithm?
        get() = router.algorithmLevel ?: AlgorithmLevel.default

    var level = initialLevel
        set(value) {
            field = value
            submitAsync { Database.INSTANCE.updatePlayerRouter(this@PlayerRouter) }
        }

    var experience = initialExperience
        set(value) {
            field = value
            submitAsync { Database.INSTANCE.updatePlayerRouter(this@PlayerRouter) }
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
            this.experience += value
            return CompletableFuture.completedFuture(null)
        }
        if (level >= algo.maxLevel) {
            level = algo.maxLevel
            algo.getExp(player, level).thenAccept {
                this.experience = it
            }
            return CompletableFuture.completedFuture(null)
        }
        val future = CompletableFuture<Void>()
        var lvl = level
        var exp = experience + value
        var expNextLevel = 0
        fun getNextLevel() = algo.getExp(player, lvl).thenAccept {
            expNextLevel = if (it <= 0) {
                Int.MAX_VALUE
            } else {
                it
            }
        }

        fun finish() {
            if (lvl >= algo.maxLevel) {
                level = algo.maxLevel
                experience = expNextLevel
            } else {
                level = lvl
                experience = exp
                if (value <= 0) {
                    addExperience(value, player)
                }
            }
            future.complete(null)
        }

        fun process() {
            getNextLevel().thenAccept {
                if (exp >= expNextLevel) {
                    lvl += 1
                    exp -= expNextLevel
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
            this.experience = maxOf(this.experience - value, 0)
            return CompletableFuture.completedFuture(null)
        }
        if (level <= algo.minLevel) {
            level = algo.minLevel
            experience = maxOf(this.experience - value, 0)
            return CompletableFuture.completedFuture(null)
        }
        val future = CompletableFuture<Void>()
        var lvl = level
        var exp = experience - value
        var expLastLevel = 0
        fun getNextLevel() = algo.getExp(player, lvl - 1).thenAccept {
            expLastLevel = if (it <= 0) {
                Int.MAX_VALUE
            } else {
                it
            }
        }

        fun finish() {
            if (lvl <= algo.minLevel) {
                level = algo.minLevel
                experience = maxOf(exp, 0)
            } else {
                level = lvl
                experience = exp
                if (value <= 0) {
                    addExperience(value, player)
                }
            }
            future.complete(null)
        }

        fun process() {
            getNextLevel().thenAccept {
                if (exp < 0) {
                    lvl -= 1
                    exp += expLastLevel
                    process()
                } else {
                    finish()
                }
            }
        }
        process()
        return future
    }
}
