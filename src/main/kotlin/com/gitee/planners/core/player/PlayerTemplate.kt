package com.gitee.planners.core.player

import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.common.metadata.MetadataContainer
import com.gitee.planners.api.common.metadata.metadataValue
import com.gitee.planners.api.job.KeyBinding
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.config.Leveling
import com.gitee.planners.core.config.level.AlgorithmLevel
import com.gitee.planners.core.database.Database
import org.bukkit.entity.Player
import taboolib.common.platform.function.submitAsync
import taboolib.common5.cfloat
import java.util.concurrent.CompletableFuture

class PlayerTemplate(val id: Long, val onlinePlayer: Player, route: PlayerRoute?, map: Map<String, Metadata>) :
    MetadataContainer(map), Leveling {

    var route = route
        set(value) {
            // 如果是要清空 route 则删除当前的技能
            if (value == null && field != null) {
                val skills = field!!.getImmutableSkillValues().mapNotNull { field!!.getSkillOrNull(it) }
                // 删除已经学习的技能
                if (skills.isNotEmpty()) {
                    submitAsync {
                        Database.INSTANCE.deleteSkill(*skills.toTypedArray())
                    }
                }
            }
            // 赋值
            field = value
            // 保存 route
            submitAsync {
                Database.INSTANCE.updateRoute(this@PlayerTemplate)
            }
        }

    var level: Int
        @JvmName("level0")
        get() = this[AlgorithmLevel.getStoragePathInIsolation(this, "level")]?.asInt()
            ?: AlgorithmLevel.getInstance(this).minLevel
        @JvmName("level1")
        private set(value) {
            this[AlgorithmLevel.getStoragePathInIsolation(this, "level")] = metadataValue(value)
        }

    var experience: Int
        @JvmName("experience0")
        get() = this[AlgorithmLevel.getStoragePathInIsolation(this, "experience")]?.asInt() ?: 0
        @JvmName("experience1")
        private set(value) {
            this[AlgorithmLevel.getStoragePathInIsolation(this, "experience")] = metadataValue(value)
        }

    val experienceMax: Int
        get() = AlgorithmLevel.getInstance(this).getExp(onlinePlayer, level).getNow(Int.MAX_VALUE)

    override fun setLevel(level: Int) {
        val algorithm = AlgorithmLevel.getInstance(this)
        // 同步 exp
        val progress = maxOf(0f, minOf(1.0f, experience / experienceMax.cfloat))
        this.level = maxOf(algorithm.minLevel, minOf(algorithm.maxLevel, level))
        this.experience = (progress * experienceMax).toInt()
    }

    override fun addLevel(value: Int) {
        this.setLevel(level + value)
    }

    override fun getLevel(): Int {
        return level
    }

    override fun setExperience(experience: Int) {

        this.experience = experience
    }


    override fun getExperience(): Int {
        return experience
    }

    override fun addExperience(value: Int): CompletableFuture<Void> {
        val algorithm = AlgorithmLevel.getInstance(this)
        if (level >= algorithm.maxLevel) {
            level = algorithm.maxLevel
            algorithm.getExp(onlinePlayer, level).thenAccept {
                this.experience = it
            }
            return CompletableFuture.completedFuture(null)
        }
        val future = CompletableFuture<Void>()
        var lvl = level
        var exp = experience + value
        var expNextLevel = 0
        fun getNextLevel() = algorithm.getExp(onlinePlayer, lvl).thenAccept {
            expNextLevel = if (it <= 0) Int.MAX_VALUE else it
        }

        fun finish() {
            if (lvl >= algorithm.maxLevel) {
                level = algorithm.maxLevel
                experience = expNextLevel
            } else {
                level = lvl
                experience = exp
                // 修正
                if (value <= 0) {
                    addExperience(value)
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

    override fun takeExperience(value: Int): CompletableFuture<Void> {
        val algorithm = AlgorithmLevel.getInstance(this)
        if (level <= algorithm.minLevel) {
            level = algorithm.minLevel
            experience = maxOf(this.experience - value, 0)
            return CompletableFuture.completedFuture(null)
        }
        val future = CompletableFuture<Void>()
        var lvl = level
        var exp = experience - value
        var expLastLevel = 0
        fun getNextLevel() = algorithm.getExp(onlinePlayer, lvl - 1).thenAccept {
            expLastLevel = if (it <= 0) Int.MAX_VALUE else it
        }

        fun finish() {
            if (lvl <= algorithm.minLevel) {
                level = algorithm.minLevel
                experience = maxOf(exp, 0)
            } else {
                level = lvl
                experience = exp
                // 修正
                if (value <= 0) {
                    addExperience(value)
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
        if (!route!!.hasImmutableSkill(id)) {
            error("The skill $id does not exist in the route ${route!!.id}")
        }
        // 如果没有学习 则添加一个新的（这时候并未在数据库内添加）
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

    fun getRegisteredSkillOrNull(binding: KeyBinding): PlayerSkill? {
        return getRegisteredSkill().values.firstOrNull { it.binding == binding }
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
        // 如果 route 不存在
        if (route == null) {
            error("You must specify a route")
        }
        return route!!.getRegisteredSkill()
    }

}
