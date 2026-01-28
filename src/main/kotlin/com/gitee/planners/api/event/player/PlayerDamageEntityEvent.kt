package com.gitee.planners.api.event.player

import com.gitee.planners.api.damage.DamageCause
import com.gitee.planners.api.damage.ProxyDamage
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import taboolib.platform.type.BukkitProxyEvent

class PlayerDamageEntityEvent(
    val player: Player,
    val entity: Entity,
    val damage: Double,
    val bukkitCause: EntityDamageEvent.DamageCause,
    val proxyDamage: ProxyDamage? = null
) : BukkitProxyEvent() {

    /** 获取伤害原因（优先使用 proxyDamage） */
    val cause: DamageCause
        get() = proxyDamage?.cause ?: DamageCause.of(bukkitCause)
}
