package com.gitee.planners.module.compat.mob

import com.gitee.planners.api.event.player.PlayerDamageEntityEvent
import com.gitee.planners.util.checkPluginClass
import io.lumine.xikage.mythicmobs.MythicMobs
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.info
import taboolib.common.util.unsafeLazy
import taboolib.platform.util.attacker

object MythicMobDamageHandler {

    private val isEnable by unsafeLazy {
        checkPluginClass("io.lumine.xikage.mythicmobs.MythicMobs")
    }

    @SubscribeEvent
    fun e(e: PlayerDamageEntityEvent) {
        info("MythicMobDamageHandler ")
        info(" isEnable: $isEnable")
        info(" damager: ${e.player}")
        if (isEnable && e.cause == EntityDamageEvent.DamageCause.CUSTOM) {
            val instance = MythicMobs.inst().mobManager.getMythicMobInstance(e.entity)
            info(" instance: $instance")
            if (instance == null) {
                return
            }
            if (instance.threatTable == null) {
                return
            }
            instance.threatTable.threatGain(BukkitAdapter.adapt(e.player), e.damage)
            info("Threat gain: ${e.damage} by ${e.player.name} to ${e.entity.name}")
        }
    }

}