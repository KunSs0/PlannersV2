package com.gitee.planners.module.binding

import com.germ.germplugin.api.event.GermKeyDownEvent
import com.germ.germplugin.api.event.GermKeyUpEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.event.player.PlayerToggleSprintEvent
import org.bukkit.inventory.EquipmentSlot
import taboolib.common.platform.event.OptionalEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.platform.event.PlayerJumpEvent
import taboolib.platform.util.isLeftClick
import taboolib.platform.util.isRightClick

object DefaultListener {

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

    @SubscribeEvent(bind = "com.germ.germplugin.api.event")
    fun handleKeydown(opt: OptionalEvent) {
        val e = opt.get<GermKeyDownEvent>()
        InteractionActionKey(e.keyType.name,InteractionActionKey.Type.PRESS)
    }

    @SubscribeEvent(bind = "com.germ.germplugin.api.event")
    fun handleKeyup(opt: OptionalEvent) {
        val e = opt.get<GermKeyUpEvent>()
        InteractionActionKey(e.keyType.name,InteractionActionKey.Type.RELEASE)
    }
}
