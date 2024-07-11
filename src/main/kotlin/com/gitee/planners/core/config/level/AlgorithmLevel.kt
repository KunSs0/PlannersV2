package com.gitee.planners.core.config.level

import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.Registries
import com.gitee.planners.api.event.player.PlayerExperienceEvent
import com.gitee.planners.api.event.player.PlayerLevelChangeEvent
import com.gitee.planners.api.event.player.PlayerProfileLoadedEvent
import com.gitee.planners.core.player.PlayerTemplate
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerExpChangeEvent
import taboolib.common.platform.event.SubscribeEvent
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
        syncingUpdated(e.template)
    }

    @SubscribeEvent(ignoreCancelled = true)
    internal fun e(e: PlayerExperienceEvent.Increment) {
        syncingUpdated(e.template)
    }


    @SubscribeEvent(ignoreCancelled = true)
    internal fun e(e: PlayerExperienceEvent.Decrement) {
        syncingUpdated(e.template)
    }

    @SubscribeEvent(ignoreCancelled = true)
    internal fun e(e: PlayerExperienceEvent.Set) {
        syncingUpdated(e.template)
    }

    @SubscribeEvent(ignoreCancelled = true)
    internal fun e(e: PlayerLevelChangeEvent) {
        syncingUpdated(e.template, e.to)
    }

    @SubscribeEvent
    fun e(e: PlayerExpChangeEvent) {
        if (originHook) {
            PlayerTemplateAPI.addExperience(e.player, e.amount)
            e.amount = 0
        }
    }

    fun syncingUpdated(player: Player) {
        syncingUpdated(player.plannersTemplate)
    }

    fun syncingUpdated(template: PlayerTemplate, level: Int) {
        val player = template.onlinePlayer
        if (synchronize) {
            player.level = level
            val progress = maxOf(0f, minOf(1f, template.experience.toFloat() / template.experienceMax.toFloat()))
            player.exp = progress
        }
    }

    /**
     * 同步更新
     */
    fun syncingUpdated(template: PlayerTemplate) {
        syncingUpdated(template, template.level)
    }

    fun getInstance(template: PlayerTemplate): Algorithm {
        return if (isolation.get() == Isolation.ROUTER && template.route != null) {
            template.route!!.router.algorithmLevel ?: error("Player router algorithm is null.")
        } else if (isolation.get() == Isolation.JOB && template.route != null) {
            template.route!!.algorithmLevel ?: error("Player job algorithm is null.")
        } else {
            default ?: error("Default algorithm is null.")
        }
    }

    fun getStoragePathInIsolation(template: PlayerTemplate, node: String): String {
        val builder = StringBuilder("@$node")
        if (isolation.get() == Isolation.ROUTER && template.route != null) {
            builder.append(".${template.route!!.router.id}")
        }
        if (isolation.get() == Isolation.JOB && template.route != null) {
            builder.append(".${template.route!!.router.id}.${template.route!!.bindingId}")
        }
        return builder.toString()
    }

    enum class Isolation {

        ALL,
        ROUTER,
        JOB

    }

}
