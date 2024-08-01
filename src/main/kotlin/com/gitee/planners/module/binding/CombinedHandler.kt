package com.gitee.planners.module.binding

import com.gitee.planners.api.PlannersAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.event.action.CombinedEvent
import com.gitee.planners.api.job.KeyBinding
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.event.player.PlayerToggleSprintEvent
import org.bukkit.inventory.EquipmentSlot
import taboolib.common.platform.event.SubscribeEvent
import taboolib.platform.event.PlayerJumpEvent
import taboolib.platform.util.isLeftClick

object CombinedHandler {

    @SubscribeEvent
    fun handleSneaking(e: PlayerToggleSneakEvent) {
        val type = if (e.isSneaking) {
            InteractionActionBukkitType.MISC_SNEAK
        } else {
            InteractionActionBukkitType.MISC_STAND_UP
        }
        CombinedAnalyzer.processAction(e.player, type)
    }

    @SubscribeEvent
    fun handleSprinting(e: PlayerToggleSprintEvent) {
        val type = if (e.isSprinting) {
            InteractionActionBukkitType.MISC_SPRING
        } else {
            InteractionActionBukkitType.MISC_WALK
        }
        CombinedAnalyzer.processAction(e.player, type)
    }

    @SubscribeEvent
    fun handleJump(e: PlayerJumpEvent) {
        CombinedAnalyzer.processAction(e.player, InteractionActionBukkitType.MISC_JUMP)
    }

    @SubscribeEvent
    fun e(e: PlayerInteractEvent) {
        if (e.hand == EquipmentSlot.HAND) {
            val type = if (e.isLeftClick()) {
                InteractionActionBukkitType.INTERACT_LEFT
            } else {
                InteractionActionBukkitType.INTERACT_RIGHT
            }
            CombinedAnalyzer.processAction(e.player, type)
        }
    }

    @SubscribeEvent
    fun e(e: CombinedEvent.Close) {
        val combined = e.combined
        if (combined is KeyBinding) {
            val skill = e.player.plannersTemplate.getRegisteredSkillOrNull(combined)
            if (skill != null) {
                PlannersAPI.cast(e.player, skill)
            }
        }
    }
}
