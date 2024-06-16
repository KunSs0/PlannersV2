package com.gitee.planners.core.config.level

import com.gitee.planners.api.ProfileAPI
import com.gitee.planners.api.ProfileAPI.plannersProfile
import com.gitee.planners.api.Registries
import com.gitee.planners.api.event.player.PlayerExperienceEvent
import com.gitee.planners.api.event.player.PlayerLevelChangeEvent
import com.gitee.planners.api.event.player.PlayerProfileLoadedEvent
import com.gitee.planners.core.player.PlayerProfile
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerExpChangeEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common5.cfloat
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.lazyConversion

object AlgorithmLevel {

    @ConfigNode("settings.level.algorithm")
    private val __default__ = "-"

    @ConfigNode("settings.level.original-hook")
    private val originHook = true

    @ConfigNode("settings.level.synchronize")
    val synchronize = false

    @ConfigNode("settings.level.isolation")
    val isolation = lazyConversion<String?, Isolation> {
        Isolation.valueOf(this?.uppercase() ?: "ALL")
    }

    val default: Algorithm?
        get() = Registries.LEVEL.getOrNull(__default__)

    @SubscribeEvent
    internal fun e(e: PlayerProfileLoadedEvent) {
        syncingUpdated(e.profile)
    }

    @SubscribeEvent(ignoreCancelled = true)
    internal fun e(e: PlayerExperienceEvent.Increment) {
        syncingUpdated(e.profile)
    }


    @SubscribeEvent(ignoreCancelled = true)
    internal fun e(e: PlayerExperienceEvent.Decrement) {
        syncingUpdated(e.profile)
    }

    @SubscribeEvent(ignoreCancelled = true)
    internal fun e(e: PlayerExperienceEvent.Set) {
        syncingUpdated(e.profile)
    }

    @SubscribeEvent(ignoreCancelled = true)
    internal fun e(e: PlayerLevelChangeEvent) {
        syncingUpdated(e.profile, e.to)
    }

    @SubscribeEvent
    fun e(e: PlayerExpChangeEvent) {
        if (originHook) {
            ProfileAPI.addExperience(e.player, e.amount)
            e.amount = 0
        }
    }

    fun syncingUpdated(player: Player) {
        syncingUpdated(player.plannersProfile)
    }

    fun syncingUpdated(profile: PlayerProfile, level: Int) {
        val player = profile.onlinePlayer
        if (synchronize) {
            player.level = level
            val progress = maxOf(0f, minOf(1f, profile.experience.toFloat() / profile.experienceMax.toFloat()))
            player.exp = progress
        }
    }

    /**
     * 同步更新
     */
    fun syncingUpdated(profile: PlayerProfile) {
        syncingUpdated(profile, profile.level)
    }

    fun getInstance(profile: PlayerProfile): Algorithm {
        if (isolation.get() == Isolation.ALL && profile.route == null) {
            error("Player route is null.")
        }
        return if (isolation.get() != Isolation.ALL) {
            profile.route!!.algorithmLevel ?: default ?: error("Player route algorithm is null.")
        } else {
            default ?: error("Default algorithm is null.")
        }
    }

    fun getStoragePathInIsolation(profile: PlayerProfile, node: String): String {
        val builder = StringBuilder("@$node")
        if (isolation.get() == Isolation.ROUTER) {
            builder.append(".${profile.route!!.router.id}")
        }
        if (isolation.get() == Isolation.JOB) {
            builder.append(".${profile.route!!.router.id}.${profile.route!!.bindingId}")
        }
        return builder.toString()
    }

    enum class Isolation {

        ALL, ROUTER, JOB

    }

}
