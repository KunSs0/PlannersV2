package com.gitee.planners.api.event.player

import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

class PlayerDamageEntityEvent(
    val player: Player,
    val entity: Entity,
    val damage: Double,
    val cause: org.bukkit.event.entity.EntityDamageEvent.DamageCause
) : BukkitProxyEvent()