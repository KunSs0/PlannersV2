package com.gitee.planners.core.config.level

import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.Registries
import com.gitee.planners.api.event.player.PlayerExperienceEvent
import com.gitee.planners.api.event.player.PlayerLevelChangedEvent
import com.gitee.planners.api.event.player.PlayerProfileLoadedEvent
import com.gitee.planners.core.player.PlayerTemplate
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerExpChangeEvent
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.configuration.ConfigNode

object AlgorithmLevel {

    @ConfigNode("settings.level.algorithm")
    private val __default__ = "-"

    @ConfigNode("settings.level.original-hook")
    private val originHook = true

    @ConfigNode("settings.level.synchronize")
    val synchronize = false

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

    /**
     * 等级提交后把实际等级同步到原版等级。
     *
     * 必须使用提交后的通知：提交前的目标等级没有经过职业算法上下限约束，
     * 直接写进原版等级会让按等级查表的属性换算拿到越界值。
     * 使用 LOWEST 让原版等级先落定，后续按等级重算属性的监听读到同一份值。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    internal fun e(e: PlayerLevelChangedEvent) {
        syncingUpdated(e.template)
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

    /**
     * 把等级同步到原版等级，并遵循当前职业算法的等级边界。
     *
     * 原版等级是下游属性换算按等级查表的输入；越界值会让换算直接失败，
     * 因此任何调用路径都先按算法上下限夹紧再写入。
     *
     * @param template 目标档案。
     * @param level 待同步的等级。
     */
    fun syncingUpdated(template: PlayerTemplate, level: Int) {
        val player = template.onlinePlayer
        if (!synchronize) {
            return
        }
        val router = template.playerRouter
        var effective = level
        if (router != null) {
            effective = maxOf(router.minLevel, minOf(router.maxLevel, level))
        }
        player.level = effective
        val progress = maxOf(0f, minOf(1f, template.experience.toFloat() / template.experienceMax.toFloat()))
        player.exp = progress
    }

    /**
     * 同步更新
     */
    fun syncingUpdated(template: PlayerTemplate) {
        syncingUpdated(template, template.level)
    }

}
